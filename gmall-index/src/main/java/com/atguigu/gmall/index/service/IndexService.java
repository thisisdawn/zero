package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.untils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";
    private static final String Lock_PREFIX = "index:cates:lock:";

    public List<CategoryEntity> queryLvl1Categories() {
        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.queryCategoriesByPid(0l);
        return responseVo.getData();
    }

    @GmallCache(prefix =KEY_PREFIX,timeout =129600,random = 14400,lock =Lock_PREFIX )
    public List<CategoryEntity> queryLvl23CatesByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.querySubCategoriesWithSubsByPid(pid);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        System.out.println("目标方法................");
        return categoryEntities;
    }



    public List<CategoryEntity> queryLvl23CatesByPid2(Long pid) {
        // 1.先查缓存，如果命中则直接返回
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class); //反序列化
        }

        //为了防止缓存击穿, 添加分布式锁
        RLock lock = this.redissonClient.getLock(Lock_PREFIX + "pid");
        lock.lock();

        try {
            //再次查询缓存. 在获取锁的过程中, 可能有其他请求已经把数据放入缓存中了.
            String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(json2)) {
                return JSON.parseArray(json2, CategoryEntity.class); //反序列化
            }


            // 2.没有命中则远程调用，获取结果集放入缓存
            ResponseVo<List<CategoryEntity>> responseVo = this.pmsClient.querySubCategoriesWithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = responseVo.getData();

            // 为了防止缓存穿透，数据即使为null也缓存，缓存时间5min
            if (CollectionUtils.isEmpty(categoryEntities)) {
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            } else {
                // 为了防止缓存雪崩，给过期时间添加随机值
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 90 + new Random().nextInt(10), TimeUnit.DAYS);
            }
            return categoryEntities;
        } finally {
            lock.unlock();
        }
    }




    public void testLock3() {
        // 加锁
        String uuid = UUID.randomUUID().toString();
        Boolean flag = this.distributedLock.tryLock("lock", uuid, 30);
        if (flag) {
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            try {
                TimeUnit.SECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //this.testSubLock("lock", uuid);

            this.distributedLock.unlock("lock", uuid);
        }
    }

    public void testSubLock(String lockName, String uuid) {
        this.distributedLock.tryLock(lockName, uuid, 30);
        // TODO
        this.distributedLock.unlock(lockName, uuid);
    }

    public void testLock2() {
        // 加锁
        String uuid = UUID.randomUUID().toString();
        while (!this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS)) {
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // this.redisTemplate.expire("lock", 10, TimeUnit.SECONDS);
        String json = this.redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(json)) {
            this.redisTemplate.opsForValue().set("num", "1");
        }
        int num = Integer.parseInt(json);
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        // 解锁 防止误删 删除之前先判断是否自己的锁
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
//        if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))){
//            this.redisTemplate.delete("lock");
//        }
    }



    //redisson框架实现分布锁
    public void testLock() {
        // 加锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();

        try {
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        } finally {
            lock.unlock();
        }


    }

    //测试读写锁
    public void testRead(){
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);
        System.out.println(".............");
    }
    public void testWrite(){
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);
        System.out.println(".............");
    }

    //测试闭锁(倒计时锁)
    public void testLatch(){
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("cdl");
        cdl.trySetCount(7);
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("票卖完了...");
    }
    public void testCountDown(){
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("cdl");
        cdl.countDown();
        System.out.println("卖了一张票...");
    }
}

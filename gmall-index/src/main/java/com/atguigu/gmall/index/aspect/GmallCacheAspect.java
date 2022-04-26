package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import jodd.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemlate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter<String> bloomFilter;

    /**
     * 目标方法的形参列表：joinPoint.getArgs()
     * 目标方法所在类：joinPoint.getTarget().getClass()
     * 目标方法的签名：MethodSignature signature = (MethodSignature)joinPoint.getSignature()
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object gmallCache(ProceedingJoinPoint joinPoint) throws Throwable {
//        获取目标方法的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//         获取目标方法对象
        Method method = signature.getMethod();
//        获取目标方法的GmallCache注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
//        获取注解中的缓存前缀
        String prefix = gmallCache.prefix();
//        获取目标方法的形参列表
        Object[] args = joinPoint.getArgs();
        String argString = StringUtils.join(args, ",");
//      组装缓存的key
        String key = prefix + argString;


//        为了防止缓存穿透, 使用布隆过滤器
        if (!bloomFilter.contains(key)) {
            return null;
        }


        // 1.先查缓存，如果命中则直接返回
        String json = this.redisTemlate.opsForValue().get(key);
        if (StringUtil.isNotBlank(json)){
            return JSON.parseObject(json,signature.getReturnType()); //目标方法的返回值类型
        }

        // 2.为了防止缓存击穿, 添加分布式锁
        String lock = gmallCache.lock();
        RLock fairLock = this.redissonClient.getFairLock(lock + argString);
        fairLock.lock();

        try {
            // 3.再次查询缓存. 在获取锁的过程中, 可能有其他请求已经把数据放入缓存中了.
            String json2 = this.redisTemlate.opsForValue().get(key);
            if (StringUtil.isNotBlank(json)) {
                return JSON.parseObject(json2, signature.getReturnType());
            }

            // 4.执行目标方法
            Object result = joinPoint.proceed(args);

            // 5.放入缓存
            if (result != null) {
                // 为了防止缓存雪崩给过期时间添加了随机值
                int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                this.redisTemlate.opsForValue().set(key,JSON.toJSONString(result),timeout, TimeUnit.MINUTES);
            }
            return result;
        } finally {
            fairLock.unlock();
        }
    }
}

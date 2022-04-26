package com.atguigu.gmall.wms.service.impl;

import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String KEY_PREFIX = "stock:info:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkLock(List<SkuLockVo> lockVos, String orderToken) {

        // 如果商品列表为空，则抛出异常
        if (CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("要购买的商品不能为空");
        }

        // 遍历所有商品，验库存并锁库存
        lockVos.forEach(lockVo -> {
            this.checkAndLock(lockVo);
        });

        // 判断是否存在锁定失败的商品，任何一个商品锁定失败，则解锁所有锁定成功了的商品库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            // 获取锁定成功的锁定信息
            lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList()).forEach(lockVo -> {
                // 遍历所有锁定成功的锁定信息，解锁库存
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });
            // 如果锁定失败 则返回锁定信息
            return lockVos;
        }

        // 为了方便将来解锁库存 或者 减库存，这里需要缓存订单的锁定信息：orderToken ： lockVos
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos), 36, TimeUnit.HOURS);

        // 发送延时消息定时解锁库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.ttl", orderToken);

        // 如果锁定成功了，则返回null
        return null;
    }

    /**
     * 针对每一个商品验库存并锁库存
     * @param lockVo
     */
    private void checkAndLock(SkuLockVo lockVo){
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + lockVo.getSkuId());
        fairLock.lock();

        try {
            // 1.验库存
            List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.check(lockVo.getSkuId(), lockVo.getCount());
            // 如果没有一个仓库满足购买要求，则验库存锁库存失败
            if (CollectionUtils.isEmpty(wareSkuEntities)){
                lockVo.setLock(false);
                return;
            }

            // 2.锁库存。按区域 或者 就近 或者 根据大数据接口
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            if (this.wareSkuMapper.lock(wareSkuEntity.getId(), lockVo.getCount()) == 1) {
                // 如果锁定库存成功，设置为true
                lockVo.setLock(true);
                lockVo.setWareSkuId(wareSkuEntity.getId());
            } else {
                lockVo.setLock(false);
            }
        } finally {
            fairLock.unlock();
        }
    }

}
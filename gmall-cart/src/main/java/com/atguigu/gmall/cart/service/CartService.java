package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.exception.CartException;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptors.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private static final String key_prefix = "cart:info:";
    private static final String price_prefix = "cart:price:";

    //获取userId方法
    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    //    保存购物车
    public void saveCart(Cart cart) {
//        1. 获取登录状态
        String userId = this.getUserId();

//        2. 获取该用户的购物车 Map<skuId,cartJson>
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key_prefix + userId); //指定外层key, 生成操作对象, 操作内部map

//        3. 判断当前用户的购物车是否有该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
//           有则更新数量
            String json = hashOps.get(skuId).toString();
            cart = JSON.parseObject(json, Cart.class);
            cart.setCount(cart.getCount().add(count));
//            保存到数据库
            this.cartAsyncService.updateCart(userId, cart.getSkuId(), cart);

        } else {
            //    无则新增记录
            cart.setUserId(userId);
            cart.setCheck(true);

            //根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new CartException("您添加的商品不存在!");
            }
//            设置sku相关参数
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());
//            查询库存
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.getSku(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
//            查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo = this.pmsClient.querySaleAttrBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrsResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
//            查询营销属性
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

//            新增购物车时, 同时新增实时价格缓存
            this.redisTemplate.opsForValue().set(price_prefix + skuId, skuEntity.getPrice().toString());

//            保存到数据库
            this.cartAsyncService.insertCart(userId,cart);
        }
//        保存到redis
        hashOps.put(skuId, JSON.toJSONString(cart));
    }


    //查询购物车
    public Cart queryCart(Long skuId) {

//        获取登录信息
        String userId = this.getUserId();

//        内层的map<skuId, cartJson>
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key_prefix + userId);
        if (!hashOps.hasKey(skuId.toString())) {
            throw new CartException("您没有该购物车记录");
        }
        String cartJson = hashOps.get(skuId.toString()).toString();
        return JSON.parseObject(cartJson, Cart.class);

    }

    @Async
    public void test1() {
        try {
            System.out.println("test1测试方法开始...........");
            TimeUnit.SECONDS.sleep(5);
            int i = 1 / 0;
            System.out.println("test1测试方法结束...........");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public void test2() {
        try {
            System.out.println("test2测试方法开始...........");
            TimeUnit.SECONDS.sleep(7);
            System.out.println("test2测试方法结束...........");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<Cart> queryCarts() {
//        获取登录信息中的userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();

//        以userKey查询未登录的购物车, Map<skuId, CartJson>
        BoundHashOperations<String, Object, Object> unloginHashOps = this.redisTemplate.boundHashOps(key_prefix + userKey);
//        获取未登录购物车的json字符串集合
        List<Object> unLoginCartJsons = unloginHashOps.values();
//        把json字符串集合 转化为cart对象集合
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(unLoginCartJsons)) {
            unLoginCarts = unLoginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询购物车, 查询实时价格缓存
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());

        }
//        判断userId是否为空, 如果为空, 直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return unLoginCarts;
        }
//        如果不为空, 合并未登录的购物车 到 已登录的购物车中
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(key_prefix + userId);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> { //未登录购物车对象
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
//                判断已登录的购物车是否包含该商品
                if (loginHashOps.hasKey(skuId)) {
                    //  包含则更新数量
                    //  获取已登录购物车对应对象
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount().add(count));
                    //  写入到数据库
                    this.cartAsyncService.updateCart(userId.toString(), cart.getSkuId(), cart);
                } else {
                    //  不包含则新增记录
                    cart.setUserId(userId.toString());
                    cart.setId(null);
                    //  写入到数据库
                    this.cartAsyncService.insertCart(userId.toString(),cart);
                }
                //  写入到redis
                loginHashOps.put(skuId, JSON.toJSONString(cart));
            });

//            清空未登录购物车
            this.redisTemplate.delete(key_prefix + userKey);
            this.cartAsyncService.deleteCart(userKey);

        }

//        查询已登录的购物车给用户
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)) {
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key_prefix + userId);
        List<Object> cartJsons = hashOps.values();
        if (!CollectionUtils.isEmpty(cartJsons)){
            return cartJsons.stream().map(cartJson ->
                JSON.parseObject(cartJson.toString(),Cart.class))
                    .filter(Cart::getCheck).collect(Collectors.toList());
        }
        return null;
    }
}

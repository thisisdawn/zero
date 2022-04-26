package com.atguigu.gmall.scheduled.xxljob;

import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String EXCEPTION_KEY = "cart:exception";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @XxlJob("cartSyncData")
    public ReturnT<String> cartSyncData(String param){

//        查询redis中的失败数据
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();
//        如果userId不为空, 则同步数据
        while(userId != null){
//         清空mysql当前用户的购物车数据
            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id",userId));
//            读取redis中的数据
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
//            判断redis中的数据是否为空, 则同步完成
            List<Object> cartJsons = hashOps.values();
            if (CollectionUtils.isEmpty(cartJsons)) {
                userId = setOps.pop();
                continue;
            }
//            把数据新增到mysql
            cartJsons.forEach(cartJson ->{
                try {
                    Cart cart = MAPPER.readValue(cartJson.toString(), Cart.class);
                    cart.setId(null);
                    this.cartMapper.insert(cart);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
//            获取下一个元素
            userId = setOps.pop();
        }

        return ReturnT.SUCCESS;
    }

}

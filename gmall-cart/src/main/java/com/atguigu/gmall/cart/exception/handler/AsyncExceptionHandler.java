package com.atguigu.gmall.cart.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
//异步异常处理器

@Component
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EXCEPTION_KEY = "cart:exception";

//    处理未捕获的异常
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
//        如果异步任务出现异常, 1.记录到数据库. 2.记录到日志
//    log.error("异步任务出现异常, 异常信息:{}, 异常方法:{}, 异常参数:{}", throwable.getMessage(),method.getName(), Arrays.asList(objects));

//        如果异步任务出现异常, 记录到redis中, 执行定时任务同步数据
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        setOps.add(objects[0].toString());
    }
}

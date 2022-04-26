package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.exception.handler.AsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class AsyncConfig implements AsyncConfigurer {

    @Autowired
    private AsyncExceptionHandler asyncExceptionHandler;

//    配置线程池
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

//    配置异步未捕获异常处理器
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncExceptionHandler;
    }
}

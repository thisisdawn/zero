package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 线程池配置类
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService(
            @Value("${threadPool.corePoolSize}") Integer corePoolSize,
            @Value("${threadPool.maximumPoolSize}") Integer maximumPoolSize,
            @Value("${threadPool.keepAliveTime}") Integer keepAliveTime,
            @Value("${threadPool.blockingQueueSize}") Integer blockingQueueSize
    ){
        return new ThreadPoolExecutor(corePoolSize,maximumPoolSize,keepAliveTime, TimeUnit.SECONDS,new ArrayBlockingQueue<>(blockingQueueSize));
    }
}

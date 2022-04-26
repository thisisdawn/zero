package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    //缓存前缀, 默认: gmall:
    String prefix() default "gmall:";

    //缓存过期时间, 单位:min, 默认: 300min
    int timeout() default 300;

    //防止缓存雪崩, 给过期时间添加随机值, 默认:60min
    int random() default 60;

    //防止缓存击穿, 给缓存添加分布式锁, 指定锁的前缀
    String lock() default "gmall:lock:";
}


package com.atguigu.gmall.index.config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//很多问题都是单词写错了, 首先仔细拼写下单词

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient1(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.52.121:6379");
        return Redisson.create(config);
    }
}

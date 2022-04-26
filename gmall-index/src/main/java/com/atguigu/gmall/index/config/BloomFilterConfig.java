package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.checkerframework.checker.units.qual.A;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

//配置布隆过滤器
@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient gmallPmsClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Bean
    public RBloomFilter<String> bloomFilter(){
//        创建布隆过滤器, 名字
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("index:bloom");
//        初始化容器和误差率
        bloomFilter.tryInit(1000l,0.03);
//        三级分类
        ResponseVo<List<CategoryEntity>> responseVo = this.gmallPmsClient.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)) {
            categoryEntities.forEach(categoryEntity -> {
                bloomFilter.add(KEY_PREFIX+categoryEntity.getId());
            });
        }
        return bloomFilter;
    }
}



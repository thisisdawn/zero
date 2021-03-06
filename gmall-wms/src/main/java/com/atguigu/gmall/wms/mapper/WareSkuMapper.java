package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * εεεΊε­
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2022-02-16 11:56:13
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {
    List<WareSkuEntity> check(@Param("skuId") Long skuId, @Param("count") Integer count);

    int lock(@Param("id") Long id, @Param("count") Integer count);

    int unlock(@Param("id") Long id, @Param("count") Integer count);

    int minus(@Param("id") Long id, @Param("count") Integer count);

}

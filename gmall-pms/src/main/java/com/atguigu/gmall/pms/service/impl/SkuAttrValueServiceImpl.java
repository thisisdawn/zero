package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValuesByCidAndSkuId(Long cid, Long skuId) {
        // 1.根据cid查询检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        // 获取attrIds集合
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 2.根据spuid和attrIds查询规格参数及值
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
    }

    @Autowired
    SkuMapper skuMapper;
    @Autowired
    SkuAttrValueMapper attrValueMapper;

    @Override
    public List<SaleAttrValueVo> querySaleAttrBySpuId(Long spuId) {
//        根据spuId查询所有的sku
        List<SkuEntity> skuEntities =  this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id",spuId));
        if (CollectionUtils.isEmpty(skuEntities)) {
            return null;
        }
//        获取所有的skuId
        List<Long> skuIds =  skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
//        根据skuIds查询销售属性集合
        List<SkuAttrValueEntity> skuAttrValuesEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
//         根据attrId对集合数据分组
        Map<Long, List<SkuAttrValueEntity>> map = skuAttrValuesEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
//        遍历分组后的map, 把每一个kv结构转换为SaleAttrValueVo
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        map.forEach((attrId,skuAttrValuesEntitiesList)->{
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            saleAttrValueVo.setAttrName(skuAttrValuesEntitiesList.get(0).getAttrName());
            saleAttrValueVo.setAttrValues(skuAttrValuesEntitiesList.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
            saleAttrValueVos.add(saleAttrValueVo);
        });

        return saleAttrValueVos;
    }

    @Override
    public String queryMappingBySpuId(Long spuId) {
        // 根据spuId查询所有的sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if (CollectionUtils.isEmpty(skuEntities)){
            return null;
        }

        // 获取skuId集合
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());

        // 查询映射关系 {暗夜黑,8G,128G: 100, 暗夜黑,12G,128G: 101}
        List<Map<String, Object>> maps = this.attrValueMapper.queryMappingBySkuIds(skuIds);
        if (CollectionUtils.isEmpty(maps)){
            return null;
        }
        // 把map的list集合 转化成map集合
        Map<String, Long> mappingMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));

        return JSON.toJSONString(mappingMap);
    }

}
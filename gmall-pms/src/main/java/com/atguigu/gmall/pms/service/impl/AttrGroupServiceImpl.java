package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private SkuAttrValueMapper saleAttrMapper;
    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SpuAttrValueMapper baseAttrMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> getAttrGroupEntity(Long cid) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id",cid);
        return this.list(wrapper);
    }


    @Override
    public List<AttrGroupEntity> getWithattrs(Long catId) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id",catId);
        List<AttrGroupEntity> groupEntities = this.list(wrapper);
        if (CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        groupEntities.forEach(AttrGroupEntity -> {
            QueryWrapper<AttrEntity> attrEntityWrapper = new QueryWrapper<>();
            attrEntityWrapper.eq("group_id",AttrGroupEntity.getId()).eq("type",1);
            List<AttrEntity> attrEntities = this.attrMapper.selectList(attrEntityWrapper);
            AttrGroupEntity.setAttrEntities(attrEntities);
        });
        return groupEntities;
    }

    @Override
    public List<ItemGroupVo> queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
        // 根据cid查询分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(groupEntities)){
            return null;
        }

        // 遍历分组查询组下的规格参数
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setId(attrGroupEntity.getId());
            itemGroupVo.setName(attrGroupEntity.getName());

            // 查询组下的规格参数
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (!CollectionUtils.isEmpty(attrEntities)){
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

                List<AttrValueVo> attrValueVos = new ArrayList<>();
                // 根据spuId结合attrIds 查询基本属性及值
                List<SpuAttrValueEntity> spuAttrValueEntities = this.baseAttrMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                    attrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                // 根据skuId结合attrIds 查询销售属性及值
                List<SkuAttrValueEntity> skuAttrValueEntities = this.saleAttrMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }

                itemGroupVo.setAttrValues(attrValueVos);
            }

            return itemGroupVo;
        }).collect(Collectors.toList());
    }
    }


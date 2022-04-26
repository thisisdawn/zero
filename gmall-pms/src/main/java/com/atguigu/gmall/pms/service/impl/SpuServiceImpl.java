package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuSaleVo;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(Long cid, PageParamVo paramVo) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();

        //添加分页查询条件
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }

        //添加关键字查询
        String key = paramVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.eq("id", key).or().like("name", key));
        }
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );
        return new PageResultVo(page);
    }

    @Autowired
    SpuDescMapper descMapper;
    @Autowired
    SpuAttrValueService baseAttrService;
    @Autowired
    SkuMapper skuMapper;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuAttrValueService saleAttrService;
    @Autowired
    GmallSmsClient gmallSmsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spuVo) {
        //保存spu相关信息
        //保存pms_spu
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
        Long spuId = spuVo.getId();
        //保存pms_spu_desc
        List<String> spuImages = spuVo.getSpuImages();
        if (!spuImages.isEmpty()) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.descMapper.insert(spuDescEntity);
        }
        //保存pms_spu_attr_value
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!baseAttrs.isEmpty()) {
            //把vo集合转换为entity集合, 然后批量保存
            this.baseAttrService.saveBatch(baseAttrs.stream().map(spuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }


        //保存sku相关信息
        List<SkuVo> skus = spuVo.getSkus();
        if (skus.isEmpty()){
            return;
        }
//        int i = 1/0;
        skus.forEach(skuVo -> {
            //保存pms_sku
            skuVo.setSpuId(spuId);
            skuVo.setBrandId(spuVo.getBrandId());
            skuVo.setCategoryId(spuVo.getCategoryId());
            //获取图片集合, 获取默认图片
            List<String> images = skuVo.getImages();
            if (!images.isEmpty()){
                skuVo.setDefaultImage(StringUtils.isBlank(skuVo.getDefaultImage())?images.get(0):skuVo.getDefaultImage());
            }
            this.skuMapper.insert(skuVo);
            Long skuId = skuVo.getId();
            //保存pms_sku_images
            //把图片地址集合, 转换为图片对象集合
            if (!images.isEmpty()){
                this.skuImagesService.saveBatch(images.stream().map(image->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(image,skuVo.getDefaultImage())?1:0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            //保存pms_sku_attr_value
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!saleAttrs.isEmpty()){
                saleAttrs.forEach(skuAttrValueEntity -> skuAttrValueEntity.setSkuId(skuId));
                this.saleAttrService.saveBatch(saleAttrs);
            }

            //保存营销相关信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSales(skuSaleVo);
        });

    }
}


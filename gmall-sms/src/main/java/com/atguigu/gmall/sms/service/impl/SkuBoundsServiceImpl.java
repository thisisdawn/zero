package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private SkuFullReductionMapper reductionMapper;
    @Autowired
    private SkuLadderMapper ladderMapper;

    @Override
    @Transactional
    public void saveSales(SkuSaleVo saleVo) {
        //保存sms_sku_bounds
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo,skuBoundsEntity);
        List<Integer> work = saleVo.getWork();
        if (!work.isEmpty()){
            skuBoundsEntity.setWork(work.get(3) * 8 + work.get(2) * 4 + work.get(1) * 2 + work.get(0));
        }
        this.save(skuBoundsEntity);

        //保存sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(saleVo.getFullAddOther());
        this.reductionMapper.insert(skuFullReductionEntity);

        //保存sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo, skuLadderEntity);
        skuLadderEntity.setAddOther(saleVo.getLadderAddOther());
        this.ladderMapper.insert(skuLadderEntity);
    }



    @Override
    public List<ItemSaleVo> querySalesBySkuId(Long skuId) {
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
//        1. 查询积分优惠
        SkuBoundsEntity skuBoundsEntity =  this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id",skuId));
        if (skuBoundsEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("积分");
            itemSaleVo.setDesc("送"+skuBoundsEntity.getGrowBounds()+"成长积分, 送"+skuBoundsEntity.getBuyBounds()+"购物积分");
            itemSaleVos.add(itemSaleVo);
        }
//        2.查询满减优惠
        SkuFullReductionEntity reductionEntity =  this.reductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id",skuId));
        if (reductionEntity != null) {
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("满减");
            itemSaleVo.setDesc("送"+reductionEntity.getFullPrice()+"成长积分, 送"+reductionEntity.getReducePrice()+"购物积分");
            itemSaleVos.add(itemSaleVo);
        }
//        3. 查询打折优惠
        SkuLadderEntity ladderEntity = this.ladderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id",skuId));
        if (ladderEntity != null) {
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("打折");
            itemSaleVo.setDesc("满"+ladderEntity.getFullCount()+"件打"+ladderEntity.getDiscount().divide(new BigDecimal(10)));
            itemSaleVos.add(itemSaleVo);
        }

        return itemSaleVos;
    }

}
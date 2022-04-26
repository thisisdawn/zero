package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;


    @Test
    void contextLoads() {
        IndexOperations indexOps = this.restTemplate.indexOps(Goods.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }

        //定义分页参数
        Integer pageNum = 1;
        Integer pageSize = 100;
        do {
            //分批查询spu
            ResponseVo<List<SpuEntity>> spuResponseVo = this.gmallPmsClient.querySpuByPageJson(new PageParamVo(pageNum, pageSize, null));
            List<SpuEntity> spuEntities = spuResponseVo.getData();
            if (spuEntities.isEmpty()) {
                return;
            }
            //如果当前页的spu不为空, 则遍历spu 查询spu下的sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsClient.querySku(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                //判断spu下的sku是否为空
                if (!skuEntities.isEmpty()) {
                    //查询品牌
                    ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsClient.queryBrandById(spuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    //查询分类
                    ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsClient.queryCategoryById(spuEntity.getCategoryId());
                    CategoryEntity categoryEntity = categoryEntityResponseVo.getData();

                    //查询基本类型的检索属性及值
                    ResponseVo<List<SpuAttrValueEntity>> baseAttrResponseVo = this.gmallPmsClient.querySearchAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                    List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrResponseVo.getData();

                    //把sku集合转换为goods集合
                    this.restTemplate.save(skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();

                        //设置sku及spu相关参数
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubtitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setCreateTime(spuEntity.getCreateTime());
                        //查询库存 设置销量和是否有货
                        ResponseVo<List<WareSkuEntity>> wareResponseVo = this.gmallWmsClient.getSku(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                        if (!wareSkuEntities.isEmpty()) {
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                        }
                        //设置品牌和分类的相关字段
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setLogo(brandEntity.getLogo());
                            goods.setBrandName(brandEntity.getName());
                        }
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        //查询销售类型的检索属性及值
                        ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.gmallPmsClient.querySearchAttrValuesByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();

                        List<SearchAttrValueVo> searchAttrs = new ArrayList<>();
                        //基本类型检索属及值 集合转换为 vo集合
                        if (spuAttrValueEntities != null) {
                            searchAttrs.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        //销售类型检索属及值 集合转换为 vo集合
                        if (skuAttrValueEntities != null) { //!CollectionUtils.isEmpty(spuAttrValueEntities 不可以用
                            searchAttrs.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        goods.setSearchAttrs(searchAttrs);
                        return goods;
                    }).collect(Collectors.toList()));
                }
            });
            //查询下一批
            pageNum++;
            pageSize = spuEntities.size();
        } while (pageSize == 100);


    }

}

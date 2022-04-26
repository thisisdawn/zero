package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySku(@PathVariable Long spuId);

//  根据spuId查询spu
    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

//    根据品牌id查询品牌
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/spuattrvalue/category/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueByCidAndSpuId(
            @PathVariable Long cid,@RequestParam("spuId") Long spuId);

    @GetMapping("pms/skuattrvalue/category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValuesByCidAndSkuId(
            @PathVariable("cid")Long cid, @RequestParam("skuId")Long skuId);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId")Long pid);

    @GetMapping("pms/category/sub/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> querySubCategoriesWithSubsByPid(@PathVariable("pid")Long pid);

    //根据skuId查询sku
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

//    根据三级分类id查询一二三级分类
    @GetMapping("pms/category/all/{cid3}")
    public ResponseVo<List<CategoryEntity>> queryLvl123CategoriesByCid3(@PathVariable("cid3") Long cid3);

//    根据skuId查询sku图片信息
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId") Long skuId);

//    根据spuId查询所有的销售属性
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrBySpuId(@PathVariable("spuId") Long spuId);

    //    根据skuId查询当前sku的销售属性
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleAttrBySkuId(@PathVariable("skuId") Long skuId);

    //    根据spuId查询销售属性组合及skuId的映射关系
    @GetMapping("pms/skuattrvalue/mapping/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId")Long spuId);

//     根据spuId查询当前spu的描述信息
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

//    根据cid查询分组及组下的规格参数和值
@GetMapping("pms/attrgroup/with/attr/value/{cid}")
public ResponseVo<List<ItemGroupVo>> queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(
        @PathVariable("cid")Long cid,
        @RequestParam("spuId")Long spuId,
        @RequestParam("skuId")Long skuId);
}

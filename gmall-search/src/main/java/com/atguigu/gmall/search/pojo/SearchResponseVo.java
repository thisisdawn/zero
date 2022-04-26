package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    // 品牌列表
    private List<BrandEntity> brands;

    // 分类列表
    private List<CategoryEntity> categories;

    // 规格参数的过滤集合
    private List<SearchResponseAttrVo> filters;

    // 总记录数
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    // 当前页的记录
    private List<Goods> goodsList;
}

package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * search.gmall.com?keyword=手机&brandId=1,2,3&categoryId=225&props=4:8G-12G&props=5:128G-256G&priceFrom=1000&priceTo=8000
 *  &store=true&sort=1&pageNum=1
 */
@Data //搜索参数对象
public class SearchParamVo {

    // 搜索关键字
    private String keyword;

    // 品牌id集合参数
    private List<Long> brandId;

    // 分类的id
    private List<Long> categoryId;

    // 规格参数的过滤条件：["5:128G-256G", "4:8G-12G"]
    private List<String> props;

    // 价格区间过滤
    private Double priceFrom;
    private Double priceTo;

    // 是否有货
    private Boolean store;

    // 排序字段：0-得分降序 1-价格降序 2-价格升序 3-销量降序 4-新品降序
    private Integer sort = 0;

    // 分页参数
    private Integer pageNum = 1;
    private final Integer pagesize = 20;


}

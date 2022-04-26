package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

// 当前spu下所有sku的销售属性
@Data
public class SaleAttrValueVo {

    private Long attrId;
    private String attrName;
    private Set<String> attrValues;
}

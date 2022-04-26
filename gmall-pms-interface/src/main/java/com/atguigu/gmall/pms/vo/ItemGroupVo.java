package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

// 规格参数
@Data
public class ItemGroupVo {

    private Long id;
    private String name;
    private List<AttrValueVo> attrValues;
}

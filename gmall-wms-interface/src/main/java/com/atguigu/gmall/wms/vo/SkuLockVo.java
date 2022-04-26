package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {
    private Long skuId; // 验库存的商品id
    private Integer count; // 要购买的数量
    private Boolean lock; // 锁定状态。true-锁定成功
    private Long wareSkuId; // 验库存并库存成功的情况下，记录锁定仓库的id
}

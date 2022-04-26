package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    // 防重的唯一标识
    private String orderToken;
    // 用户选中的收货地址
    private UserAddressEntity address;
    // 支付方式
    private Integer payType;
    // 物流公司
    private String deliveryCompany;
    // 购物积分
    private Integer bounds;
    // 送货清单
    private List<OrderItemVo> items;
    // 总金额。验总价所需的字段
    private BigDecimal totalPrice;
}

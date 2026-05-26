package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class OrderResponse {
    private Integer orderId;
    private BigDecimal totalAmount;
    private String status;
    private String message = "Order placed successfully!";
}
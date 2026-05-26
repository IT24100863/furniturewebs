package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class OrderSummaryResponse {
    private Integer orderId;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private String status;
    private List<String> itemNames;   // simple summary for list view
}
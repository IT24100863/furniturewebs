package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrderAdminResponse {
    private Integer orderId;
    private String customerName;
    private String customerEmail;
    private String address;
    private List<String> items;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String orderStatus;
    private LocalDateTime orderDate;
}
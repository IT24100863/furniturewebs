package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class DeliveryResponse {
    private Integer deliveryId;
    private Integer orderId;
    private String customerName;
    private String customerEmail;
    private String trackingCode;
    private String deliveryPartner;
    private String status;
    private LocalDateTime deliveryDate;
    private String addressSummary;
}
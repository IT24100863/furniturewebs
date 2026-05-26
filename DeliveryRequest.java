package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DeliveryRequest {
    private Integer orderId;
    private LocalDateTime deliveryDate;
    private String deliveryPartner;
}
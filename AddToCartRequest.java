package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddToCartRequest {
    private Integer productId;
    private Integer quantity;
}
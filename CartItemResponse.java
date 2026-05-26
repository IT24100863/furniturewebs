package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CartItemResponse {

    private Integer productId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private List<String> imageUrls;

}
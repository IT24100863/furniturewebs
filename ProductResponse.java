package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductResponse {
    private Integer productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String categoryName;
    private List<String> imageUrls;
    private Double averageRating;

    // NEW FIELDS FOR FULL DETAILS
    private String material;
    private String color;
    private String dimensions;
    private BigDecimal weight;

    // 🔥 ADD THIS NEW FIELD FOR OFFERS
    private Double discountPercentage;
}
package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewResponseForProfile {
    private Integer reviewId;
    private Integer productId;
    private String productName;
    private String productImageUrl;   // first image
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
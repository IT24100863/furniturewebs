package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewResponse {
    private String userName;
    private Integer rating;
    private String comment;
    private String createdAt;
}
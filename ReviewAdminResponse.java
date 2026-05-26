package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReviewAdminResponse {
    private Integer reviewId;
    private String productName;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
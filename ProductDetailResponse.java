package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ProductDetailResponse extends ProductResponse {
    private List<ReviewResponse> reviews = new ArrayList<>();
}
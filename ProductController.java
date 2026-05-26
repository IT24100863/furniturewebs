package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.ProductDetailResponse;
import com.webs.furniturewebs.dto.ProductResponse;
import com.webs.furniturewebs.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Unified endpoint – supports search, category, AND price filter
    @GetMapping
    public List<ProductResponse> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        return productService.getFilteredProducts(q, categoryId, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Integer id) {
        return productService.getProductDetail(id);
    }
}
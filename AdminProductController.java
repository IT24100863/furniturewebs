package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.AdminProductRequest;
import com.webs.furniturewebs.entity.Product;
import com.webs.furniturewebs.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> create(
            @ModelAttribute AdminProductRequest req,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        Product saved = productService.saveProduct(req, images, null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Integer id,
            @ModelAttribute AdminProductRequest req,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {

        Product saved = productService.saveProduct(req, images, id);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
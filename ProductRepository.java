package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory_CategoryId(Integer categoryId);
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
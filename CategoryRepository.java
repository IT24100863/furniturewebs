package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {}
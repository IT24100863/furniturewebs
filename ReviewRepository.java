package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByProduct_ProductId(Integer productId);
    List<Review> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    // NEW - for Admin
    List<Review> findAllByOrderByCreatedAtDesc();
}
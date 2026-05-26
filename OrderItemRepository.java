package com.webs.furniturewebs.repository;
import com.webs.furniturewebs.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {}
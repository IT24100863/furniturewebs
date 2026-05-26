package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Delivery;
import com.webs.furniturewebs.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Integer> {

    List<Delivery> findByOrder_OrderId(Integer orderId);

    Optional<Delivery> findFirstByOrder_OrderIdOrderByDeliveryDateDesc(Integer orderId);

    List<Delivery> findByStatus(DeliveryStatus status);

    // Safe method - uses the new createdAt field
    List<Delivery> findAllByOrderByCreatedAtDesc();

    Optional<Delivery> findByTrackingCode(String trackingCode);
    List<Delivery> findByOrder_User_UserId(Integer userId);
}
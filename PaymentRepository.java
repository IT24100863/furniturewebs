package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Payment;
import com.webs.furniturewebs.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Existing methods
    List<Payment> findByOrder_User_UserId(Integer userId);

    List<Payment> findByOrder_OrderId(Integer orderId);

    // Improved status search using enum
    List<Payment> findByStatus(PaymentStatus status);

    // Find by multiple statuses (useful for admin)
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);

    // Search payments by Order ID (for admin search)
    @Query("SELECT p FROM Payment p WHERE p.order.orderId = :orderId")
    List<Payment> findByOrderId(@Param("orderId") Integer orderId);

    // Optional: Find latest payment for an order
    Optional<Payment> findFirstByOrder_OrderIdOrderByTransactionDateDesc(Integer orderId);

    // For admin - pending or failed transactions
    List<Payment> findByStatusInOrderByTransactionDateDesc(List<PaymentStatus> statuses);
}
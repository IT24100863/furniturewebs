package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Order;
import com.webs.furniturewebs.entity.OrderStatus;
import com.webs.furniturewebs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Existing methods
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    List<Order> findAllByOrderByCreatedAtDesc();

    // Find orders by status (useful for admin dashboard)
    List<Order> findByStatus(OrderStatus status);

    // Find orders by multiple statuses
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    // Search orders by customer name (partial match) - for admin
    @Query("SELECT o FROM Order o WHERE LOWER(CONCAT(o.user.firstName, ' ', o.user.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR o.user.email LIKE CONCAT('%', :keyword, '%')")
    List<Order> searchByCustomerKeyword(@Param("keyword") String keyword);

    // Find recent orders (for dashboard)
    List<Order> findTop10ByOrderByCreatedAtDesc();

    // Orders with specific status ordered by date
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
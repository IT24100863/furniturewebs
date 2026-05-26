package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.LoginHistory;
import com.webs.furniturewebs.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {

    /**
     * Find all login records for a specific user, sorted by most recent first
     */
    List<LoginHistory> findByUserOrderByLoginTimeDesc(User user);

    /**
     * Optional: count total logins for a user
     */
    long countByUser(User user);

    /**
     * Optional: find logins within a time range (example for future analytics)
     */
    // List<LoginHistory> findByUserAndLoginTimeBetweenOrderByLoginTimeDesc(
    //         User user, LocalDateTime start, LocalDateTime end);
}
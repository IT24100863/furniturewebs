package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Integer> {

    // Customer methods
    List<SupportTicket> findByCustomer_UserIdOrderByCreatedAtDesc(Integer userId);

    Optional<SupportTicket> findByTicketCode(String ticketCode);

    // Admin methods
    List<SupportTicket> findAllByOrderByCreatedAtDesc();


    // Search by ticket code or subject (case-insensitive)
    List<SupportTicket> findByTicketCodeContainingIgnoreCaseOrSubjectContainingIgnoreCase(
            String ticketCode, String subject);
}
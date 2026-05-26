package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.TicketReply;
import com.webs.furniturewebs.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketReplyRepository extends JpaRepository<TicketReply, Integer> {

    List<TicketReply> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}
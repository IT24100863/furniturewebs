package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.TicketAttachment;
import com.webs.furniturewebs.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Integer> {

    List<TicketAttachment> findByTicket(SupportTicket ticket);
}
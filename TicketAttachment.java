package com.webs.furniturewebs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_attachments")
@Getter @Setter @NoArgsConstructor
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attachmentId;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    private String fileUrl;      // e.g. /uploads/tickets/12345_invoice.pdf
    private String originalName;
}
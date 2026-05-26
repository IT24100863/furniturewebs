package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class TicketResponse {
    private Integer ticketId;
    private String ticketCode;
    private String subject;
    private String requestType;
    private String status;
    private LocalDateTime createdAt;
    private int replyCount;
}
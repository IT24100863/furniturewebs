package com.webs.furniturewebs.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class TicketReplyResponse {
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}
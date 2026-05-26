package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class TicketDetailResponse {
    private Integer ticketId;
    private String ticketCode;
    private String subject;
    private String requestType;
    private String status;
    private String customerName;
    private LocalDateTime createdAt;
    private List<TicketReplyResponse> replies;
    private List<String> attachmentUrls;
}


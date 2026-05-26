package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TicketCreateRequest {
    private String contactNumber;
    private String requestType;   // Complaint, Inquiry, etc.
    private String subject;
    private String message;
}
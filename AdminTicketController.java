package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.AdminTicketResponse;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.SupportTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTicketController {

    private final SupportTicketService ticketService;

    public AdminTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<AdminTicketResponse> getAll(@RequestParam(required = false) String search) {
        return ticketService.getAllTicketsForAdmin(search);
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Integer ticketId, @RequestParam String status) {
        ticketService.updateTicketStatus(ticketId, status);
        return ResponseEntity.ok("Ticket status updated successfully");
    }

    @PostMapping("/{ticketId}/reply")
    public ResponseEntity<String> reply(
            @PathVariable Integer ticketId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User adminUser) {

        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Reply message is required");
        }

        ticketService.replyToTicket(ticketId, message, adminUser);
        return ResponseEntity.ok("Reply sent successfully");
    }
}
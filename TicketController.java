package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.TicketCreateRequest;
import com.webs.furniturewebs.dto.TicketDetailResponse;
import com.webs.furniturewebs.dto.TicketResponse;
import com.webs.furniturewebs.dto.TicketUpdateRequest;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.SupportTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final SupportTicketService ticketService;

    public TicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> create(
            @AuthenticationPrincipal User user,
            @ModelAttribute TicketCreateRequest req,
            @RequestParam(value = "attachments", required = false) MultipartFile[] files) {

        ticketService.createTicket(user, req, files);
        return ResponseEntity.ok("Ticket created successfully! Check your email for the tracking code.");
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketService.getMyTickets(user));
    }

    // Public tracking (no login required)
    @GetMapping("/track")
    public ResponseEntity<TicketDetailResponse> track(@RequestParam String code) {
        return ResponseEntity.ok(ticketService.getTicketByCode(code));
    }
    // Edit own ticket (only if status is OPEN or IN_PROGRESS)
    @PutMapping("/{ticketId}")
    public ResponseEntity<String> updateTicket(
            @AuthenticationPrincipal User user,
            @PathVariable Integer ticketId,
            @RequestBody TicketUpdateRequest req) {

        // You need to implement this in Service
        ticketService.updateTicket(user, ticketId, req);
        return ResponseEntity.ok("Ticket updated successfully");
    }

    // Delete own ticket (only if no replies yet or status OPEN)
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<String> deleteTicket(
            @AuthenticationPrincipal User user,
            @PathVariable Integer ticketId) {

        ticketService.deleteTicket(user, ticketId);
        return ResponseEntity.ok("Ticket deleted successfully");
    }
}
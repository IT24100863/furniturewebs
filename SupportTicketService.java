package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.*;
import com.webs.furniturewebs.entity.*;
import com.webs.furniturewebs.repository.SupportTicketRepository;
import com.webs.furniturewebs.repository.TicketReplyRepository;
import com.webs.furniturewebs.repository.TicketAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupportTicketService {

    private final SupportTicketRepository ticketRepo;
    private final TicketReplyRepository replyRepo;
    private final TicketAttachmentRepository attachmentRepo;
    private final EmailService emailService;
    private final String uploadDir = "uploads/tickets/";

    public SupportTicketService(
            SupportTicketRepository ticketRepo,
            TicketReplyRepository replyRepo,
            TicketAttachmentRepository attachmentRepo,
            EmailService emailService) {
        this.ticketRepo = ticketRepo;
        this.replyRepo = replyRepo;
        this.attachmentRepo = attachmentRepo;
        this.emailService = emailService;
    }

    // ====================== CREATE TICKET ======================
    public SupportTicket createTicket(User customer, TicketCreateRequest req, MultipartFile[] files) {
        SupportTicket ticket = new SupportTicket();
        ticket.setCustomer(customer);
        ticket.setTicketCode("TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setSubject(req.getSubject());
        ticket.setDescription(req.getMessage());
        ticket.setRequestType(req.getRequestType());
        ticket.setContactNumber(req.getContactNumber());
        ticket.setStatus(TicketStatus.OPEN);

        ticket = ticketRepo.save(ticket);

        // Handle attachments
        if (files != null && files.length > 0) {
            new File(uploadDir).mkdirs();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String fileName = ticket.getTicketId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        Path path = Paths.get(uploadDir + fileName);
                        Files.copy(file.getInputStream(), path);

                        TicketAttachment att = new TicketAttachment();
                        att.setTicket(ticket);
                        att.setFileUrl("/uploads/tickets/" + fileName);
                        att.setOriginalName(file.getOriginalFilename());
                        attachmentRepo.save(att);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Send email with tracking code
        emailService.sendTicketCreatedEmail(customer, ticket);

        return ticket;
    }

    // ====================== GET MY TICKETS ======================
    public List<TicketResponse> getMyTickets(User user) {
        return ticketRepo.findByCustomer_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ====================== GET TICKET BY CODE (PUBLIC) ======================
    public TicketDetailResponse getTicketByCode(String code) {
        SupportTicket ticket = ticketRepo.findByTicketCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ticket code"));

        TicketDetailResponse res = new TicketDetailResponse();
        res.setTicketId(ticket.getTicketId());
        res.setTicketCode(ticket.getTicketCode());
        res.setSubject(ticket.getSubject());
        res.setRequestType(ticket.getRequestType());
        res.setStatus(ticket.getStatus().name());
        res.setCustomerName(ticket.getCustomer().getFirstName() + " " +
                (ticket.getCustomer().getLastName() != null ? ticket.getCustomer().getLastName() : ""));
        res.setCreatedAt(ticket.getCreatedAt());

        res.setReplies(ticket.getReplies().stream().map(r -> {
            TicketReplyResponse rr = new TicketReplyResponse();
            rr.setSenderName(r.getSender().getFirstName() +
                    (r.getSender().getLastName() != null ? " " + r.getSender().getLastName() : ""));
            rr.setMessage(r.getMessage());
            rr.setCreatedAt(r.getCreatedAt());
            return rr;
        }).collect(Collectors.toList()));

        res.setAttachmentUrls(ticket.getAttachments().stream()
                .map(TicketAttachment::getFileUrl).collect(Collectors.toList()));

        return res;
    }

    // ====================== UPDATE TICKET (User can only edit OPEN tickets) ======================
    public void updateTicket(User user, Integer ticketId, TicketUpdateRequest req) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        // Security check: Only owner can edit
        if (!ticket.getCustomer().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("You can only edit your own tickets");
        }

        // Only allow editing if ticket is still OPEN or IN_PROGRESS
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("You can only edit tickets that are Open or In Progress");
        }

        if (req.getSubject() != null && !req.getSubject().trim().isEmpty()) {
            ticket.setSubject(req.getSubject().trim());
        }
        if (req.getMessage() != null && !req.getMessage().trim().isEmpty()) {
            ticket.setDescription(req.getMessage().trim());
        }

        ticketRepo.save(ticket);
    }

    // ====================== DELETE TICKET (Only if no replies and still OPEN) ======================
    public void deleteTicket(User user, Integer ticketId) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        // Security check: Only owner can delete
        if (!ticket.getCustomer().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("You can only delete your own tickets");
        }

        // Only allow deletion if ticket has no replies and is still OPEN
        if (!ticket.getReplies().isEmpty()) {
            throw new IllegalStateException("Cannot delete a ticket that has replies from support");
        }

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException("You can only delete tickets that are still Open");
        }

        ticketRepo.delete(ticket);
    }

    // ====================== HELPER MAPPER ======================
    private TicketResponse mapToResponse(SupportTicket t) {
        TicketResponse r = new TicketResponse();
        r.setTicketId(t.getTicketId());
        r.setTicketCode(t.getTicketCode());
        r.setSubject(t.getSubject());
        r.setRequestType(t.getRequestType());
        r.setStatus(t.getStatus().name());
        r.setCreatedAt(t.getCreatedAt());
        r.setReplyCount(t.getReplies().size());
        return r;
    }

    // ====================== ADMIN: GET ALL TICKETS ======================
    // ====================== ADMIN: GET ALL TICKETS ======================
    public List<AdminTicketResponse> getAllTicketsForAdmin(String search) {
        List<SupportTicket> list;

        if (search != null && !search.trim().isEmpty()) {
            // FIXED: Pass the search term TWICE (once for ticketCode, once for subject)
            list = ticketRepo.findByTicketCodeContainingIgnoreCaseOrSubjectContainingIgnoreCase(
                    search.trim(), search.trim());
        } else {
            list = ticketRepo.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(this::mapToAdminResponse).collect(Collectors.toList());
    }

    // ====================== ADMIN: UPDATE STATUS ======================
    public void updateTicketStatus(Integer ticketId, String newStatus) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        ticket.setStatus(TicketStatus.valueOf(newStatus.toUpperCase()));
        ticketRepo.save(ticket);
    }

    // ====================== ADMIN: REPLY TO TICKET ======================
    public void replyToTicket(Integer ticketId, String message, User adminUser) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        TicketReply reply = new TicketReply();
        reply.setTicket(ticket);
        reply.setSender(adminUser);
        reply.setMessage(message.trim());
        replyRepo.save(reply);

        // Optional: You can send email notification to customer here later
        System.out.println("Admin reply added to ticket #" + ticket.getTicketCode());
    }

    private AdminTicketResponse mapToAdminResponse(SupportTicket t) {
        AdminTicketResponse r = new AdminTicketResponse();
        r.setTicketId(t.getTicketId());
        r.setTicketCode(t.getTicketCode());
        r.setCustomerName(t.getCustomer().getFirstName() + " " +
                (t.getCustomer().getLastName() != null ? t.getCustomer().getLastName() : ""));
        r.setSubject(t.getSubject());
        r.setRequestType(t.getRequestType());
        r.setStatus(t.getStatus().name());
        r.setCreatedAt(t.getCreatedAt());
        return r;
    }
}
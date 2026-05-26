package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.PaymentResponse;
import com.webs.furniturewebs.entity.Payment;
import com.webs.furniturewebs.entity.PaymentStatus;
import com.webs.furniturewebs.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    public AdminPaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Get all payments (with optional search by orderId or customer)
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) String search) {

        List<Payment> payments;

        if (search != null && !search.trim().isEmpty()) {
            try {
                Integer orderId = Integer.parseInt(search.trim());
                payments = paymentRepository.findByOrder_OrderId(orderId);
            } catch (NumberFormatException e) {
                // If not number, search is ignored for now (you can enhance later)
                payments = paymentRepository.findAll();
            }
        } else {
            payments = paymentRepository.findAll();
        }

        List<PaymentResponse> response = payments.stream().map(p -> {
            PaymentResponse r = new PaymentResponse();
            r.setPaymentId(p.getPaymentId());
            r.setOrderId(p.getOrder().getOrderId());
            r.setAmount(p.getAmount());
            r.setStatus(p.getStatus().name());
            r.setPaymentMethod(p.getPaymentMethod());
            r.setTransactionDate(p.getTransactionDate());
            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Optional: Get payments by status (e.g. FAILED, PENDING)
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable String status) {
        PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
        List<Payment> payments = paymentRepository.findByStatus(paymentStatus);

        List<PaymentResponse> response = payments.stream().map(p -> {
            PaymentResponse r = new PaymentResponse();
            r.setPaymentId(p.getPaymentId());
            r.setOrderId(p.getOrder().getOrderId());
            r.setAmount(p.getAmount());
            r.setStatus(p.getStatus().name());
            r.setPaymentMethod(p.getPaymentMethod());
            r.setTransactionDate(p.getTransactionDate());
            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
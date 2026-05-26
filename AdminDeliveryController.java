package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.DeliveryResponse;
import com.webs.furniturewebs.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deliveries")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final OrderService orderService;

    public AdminDeliveryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<DeliveryResponse> getAll() {
        return orderService.getAllDeliveriesForAdmin();
    }

    @GetMapping("/ready")
    public List<DeliveryResponse> getReadyToAssign() {
        return orderService.getReadyToAssignDeliveries();
    }

    @PostMapping("/{orderId}/assign")
    public ResponseEntity<String> assignDelivery(@PathVariable Integer orderId,
                                                 @RequestBody Map<String, String> req) {
        String partner = req.get("deliveryPartner");
        String date = req.get("deliveryDate");
        orderService.assignDelivery(orderId, partner, date);
        return ResponseEntity.ok("Delivery assigned and customer notified");
    }
    @PutMapping("/{deliveryId}/reschedule")
    public ResponseEntity<String> rescheduleDelivery(@PathVariable Integer deliveryId,
                                                     @RequestBody Map<String, String> req) {
        String dateStr = req.get("deliveryDate");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Delivery date is required");
        }

        orderService.rescheduleDelivery(deliveryId, dateStr.trim());
        return ResponseEntity.ok("Delivery rescheduled successfully. Customer has been notified by email.");
    }

    @PutMapping("/{deliveryId}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Integer deliveryId,
                                               @RequestParam String status) {
        orderService.updateDeliveryStatus(deliveryId, status);
        return ResponseEntity.ok("Status updated");
    }

    @PostMapping("/{deliveryId}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Integer deliveryId) {
        orderService.cancelDelivery(deliveryId);
        return ResponseEntity.ok("Delivery cancelled and customer notified");
    }

    @GetMapping("/track")
    public ResponseEntity<DeliveryResponse> trackDelivery(@RequestParam String code) {
        return ResponseEntity.ok(orderService.trackByCode(code));
    }
}
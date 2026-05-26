package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.OrderAdminResponse;
import com.webs.furniturewebs.service.OrderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderAdminResponse> getAllOrders() {
        return orderService.getAllOrdersForAdmin();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(@PathVariable Integer id, @RequestParam String status) {
        orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok("Order status updated to " + status);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<String> refund(@PathVariable Integer id) {
        orderService.refundOrder(id);
        return ResponseEntity.ok("Refund processed successfully");
    }

    /**
     * NEW: Generate Monthly Sales Report as PDF
     */
    @GetMapping("/reports/monthly-sales")
    public ResponseEntity<byte[]> generateMonthlySalesReport() {
        byte[] pdfBytes = orderService.generateMonthlySalesReport();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "Anura_Furnitures_Monthly_Sales_Report_" +
                        java.time.LocalDate.now().toString() + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
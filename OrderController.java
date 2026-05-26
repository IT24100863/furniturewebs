package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.DeliveryResponse;
import com.webs.furniturewebs.dto.OrderResponse;
import com.webs.furniturewebs.dto.OrderResponseForProfile;
import com.webs.furniturewebs.dto.PlaceOrderRequest;
import com.webs.furniturewebs.entity.Order;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal User user,
            @RequestBody PlaceOrderRequest req) {

        Order order = orderService.placeOrder(user, req);
        OrderResponse res = new OrderResponse();
        res.setOrderId(order.getOrderId());
        res.setTotalAmount(order.getTotalAmount());
        res.setStatus(order.getStatus().name());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponseForProfile>> getMyOrders(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<OrderResponseForProfile> orders = orderService.getUserOrdersForProfile(user);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<String> cancelOrder(@AuthenticationPrincipal User user,
                                              @PathVariable Integer id) {
        orderService.cancelOrder(user, id);
        return ResponseEntity.ok("Order cancelled successfully");
    }
    @GetMapping("/track")
    public ResponseEntity<DeliveryResponse> trackDelivery(@RequestParam String code) {
        try {
            return ResponseEntity.ok(orderService.trackByCode(code));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
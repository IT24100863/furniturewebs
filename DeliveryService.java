package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.DeliveryRequest;
import com.webs.furniturewebs.dto.DeliveryResponse;
import com.webs.furniturewebs.entity.*;
import com.webs.furniturewebs.repository.DeliveryRepository;
import com.webs.furniturewebs.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepo;
    private final OrderRepository orderRepo;
    private final EmailService emailService;

    public DeliveryService(DeliveryRepository deliveryRepo, OrderRepository orderRepo, EmailService emailService) {
        this.deliveryRepo = deliveryRepo;
        this.orderRepo = orderRepo;
        this.emailService = emailService;
    }

    // View all deliveries
    public List<DeliveryResponse> getAllDeliveries() {
        return deliveryRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Confirmed orders ready for delivery creation (those without tracking code yet)
    public List<Order> getConfirmedOrdersForDelivery() {
        // Optimized query - fetch only necessary data
        return orderRepo.findByStatus(OrderStatus.CONFIRMED)
                .stream()
                .filter(order -> deliveryRepo.findByOrder_OrderId(order.getOrderId())
                        .stream().noneMatch(d -> d.getTrackingCode() != null))
                .collect(Collectors.toList());
    }

    @Transactional
    public Delivery createDelivery(DeliveryRequest req) {
        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED orders can have delivery created");
        }

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setAddress(order.getItems().get(0).getProduct() != null ? /* fallback */ null : null); // address is already linked via existing logic
        // Since existing placeOrder already created Delivery with address, we reuse it
        Delivery existing = deliveryRepo.findFirstByOrder_OrderIdOrderByDeliveryDateDesc(order.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Delivery record not found for this order"));

        existing.setDeliveryDate(req.getDeliveryDate());
        existing.setDeliveryPartner(req.getDeliveryPartner());
        existing.setTrackingCode("ANURA-DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        existing.setStatus(DeliveryStatus.SHIPPED); // initial after creation

        Delivery saved = deliveryRepo.save(existing);

        // Send email with tracking code
        emailService.sendDeliveryCreatedEmail(order.getUser(), order, saved);

        return saved;
    }

    @Transactional
    public void updateDeliveryStatus(Integer deliveryId, String newStatus) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        delivery.setStatus(DeliveryStatus.valueOf(newStatus.toUpperCase()));
        deliveryRepo.save(delivery);
    }

    @Transactional
    public void cancelDelivery(Integer deliveryId) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order");
        }

        delivery.setStatus(DeliveryStatus.CANCELLED);
        deliveryRepo.save(delivery);

        // FIXED: Pass the Delivery object, not trackingCode
        emailService.sendDeliveryCancelledEmail(
                delivery.getOrder().getUser(),
                delivery.getOrder(),
                delivery
        );
    }

    private DeliveryResponse mapToResponse(Delivery d) {
        DeliveryResponse r = new DeliveryResponse();
        r.setDeliveryId(d.getDeliveryId());
        r.setOrderId(d.getOrder().getOrderId());
        r.setCustomerName(d.getOrder().getUser().getFirstName() + " " +
                (d.getOrder().getUser().getLastName() != null ? d.getOrder().getUser().getLastName() : ""));
        r.setCustomerEmail(d.getOrder().getUser().getEmail());
        r.setTrackingCode(d.getTrackingCode());
        r.setDeliveryPartner(d.getDeliveryPartner());
        r.setDeliveryDate(d.getDeliveryDate());
        r.setStatus(d.getStatus().name());

        // Safe address mapping
        if (d.getAddress() != null) {
            Address a = d.getAddress();
            r.setAddressSummary((a.getHouseNo() != null ? a.getHouseNo() + ", " : "") +
                    (a.getStreet() != null ? a.getStreet() : "") +
                    (a.getCity() != null ? ", " + a.getCity() : "") +
                    (a.getDistrict() != null ? ", " + a.getDistrict() : ""));
        } else {
            r.setAddressSummary("No address");
        }
        return r;
    }

}
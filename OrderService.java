package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.DeliveryResponse;
import com.webs.furniturewebs.dto.OrderAdminResponse;
import com.webs.furniturewebs.dto.OrderResponseForProfile;
import com.webs.furniturewebs.dto.PlaceOrderRequest;
import com.webs.furniturewebs.entity.*;
import com.webs.furniturewebs.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepo;
    private final AddressRepository addressRepo;
    private final PaymentRepository paymentRepo;
    private final DeliveryRepository deliveryRepo;
    private final SavedCardRepository savedCardRepo;
    private final CartService cartService;
    private final AddressService addressService;
    private final EmailService emailService;

    public OrderService(OrderRepository orderRepo,
                        AddressRepository addressRepo,
                        PaymentRepository paymentRepo,
                        DeliveryRepository deliveryRepo,
                        SavedCardRepository savedCardRepo,
                        CartService cartService,
                        AddressService addressService,
                        EmailService emailService) {
        this.orderRepo = orderRepo;
        this.addressRepo = addressRepo;
        this.paymentRepo = paymentRepo;
        this.deliveryRepo = deliveryRepo;
        this.savedCardRepo = savedCardRepo;
        this.cartService = cartService;
        this.addressService = addressService;
        this.emailService = emailService;
    }

    /**
     * Main method to place an order
     */
    public Order placeOrder(User user, PlaceOrderRequest req) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // === ADDRESS ===
        Address address;
        if (req.getAddressId() != null) {
            address = addressRepo.findById(req.getAddressId())
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        } else {
            address = addressService.saveAddress(user, req);
        }

        // === ORDER ===
        Order order = new Order();
        order.setUser(user);
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(ci.getProduct().getPrice());
            order.getItems().add(oi);
        }

        order = orderRepo.save(order);

        // === PAYMENT ===
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setPaymentMethod(req.getPaymentMethod());

        if ("CARD".equals(req.getPaymentMethod())) {
            String last4 = (req.getCardNumber() != null && req.getCardNumber().length() > 4)
                    ? req.getCardNumber().substring(req.getCardNumber().length() - 4)
                    : "****";
            payment.setCardLast4(last4);
            payment.setCardType("VISA/MASTERCARD");

            if (req.isSaveCard() && req.getCardHolderName() != null && !req.getCardHolderName().trim().isEmpty()) {
                String fakeToken = "tok_fake_" + System.currentTimeMillis() + "_" + last4;
                SavedCard sc = new SavedCard(
                        user,
                        fakeToken,
                        last4,
                        "VISA/MASTERCARD",
                        req.getCardHolderName().trim(),
                        req.getExpiry() != null ? req.getExpiry().trim() : null
                );
                savedCardRepo.save(sc);
            }
        }
        paymentRepo.save(payment);

        // === DELIVERY ===
        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setAddress(address);
        deliveryRepo.save(delivery);

        // Clear cart
        cart.getItems().clear();
        cartService.getOrCreateCart(user);

        // ====================== SEND EMAILS ======================
        try {
            emailService.sendOrderConfirmation(user, order);
            emailService.sendPaymentVerificationEmail(user, order, payment);
            emailService.sendInvoiceEmail(user, order);
            System.out.println("✅ All order emails sent to " + user.getEmail());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send order emails: " + e.getMessage());
        }

        return order;
    }

    // ====================== USER PROFILE ======================
    public List<OrderResponseForProfile> getUserOrdersForProfile(User user) {
        List<Order> orders = orderRepo.findByUserOrderByCreatedAtDesc(user);
        return orders.stream().map(order -> {
            OrderResponseForProfile dto = new OrderResponseForProfile();
            dto.setOrderId(order.getOrderId());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setStatus(order.getStatus().name());
            List<String> itemNames = order.getItems().stream()
                    .map(item -> item.getQuantity() + " × " + item.getProduct().getName())
                    .collect(Collectors.toList());
            dto.setItemNames(itemNames);
            dto.setTotalItems(itemNames.size());
            return dto;
        }).collect(Collectors.toList());
    }

    // ====================== ADMIN DASHBOARD ======================
    public List<OrderAdminResponse> getAllOrdersForAdmin() {
        List<Order> orders = orderRepo.findAllByOrderByCreatedAtDesc();
        return orders.stream().map(this::mapToAdminResponse).collect(Collectors.toList());
    }

    private OrderAdminResponse mapToAdminResponse(Order order) {
        OrderAdminResponse res = new OrderAdminResponse();
        res.setOrderId(order.getOrderId());
        res.setCustomerName(order.getUser().getFirstName() + " " +
                (order.getUser().getLastName() != null ? order.getUser().getLastName() : ""));
        res.setCustomerEmail(order.getUser().getEmail());
        res.setTotalAmount(order.getTotalAmount());
        res.setOrderStatus(order.getStatus().name());
        res.setOrderDate(order.getCreatedAt());

        // Payment Status
        Payment payment = paymentRepo.findFirstByOrder_OrderIdOrderByTransactionDateDesc(order.getOrderId())
                .orElse(null);
        res.setPaymentStatus(payment != null ? payment.getStatus().name() : "PENDING");

        // Items summary
        List<String> itemNames = order.getItems().stream()
                .map(item -> item.getQuantity() + " × " + item.getProduct().getName())
                .collect(Collectors.toList());
        res.setItems(itemNames);

        // Address
        Delivery delivery = deliveryRepo.findByOrder_OrderId(order.getOrderId())
                .stream()
                .findFirst()
                .orElse(null);
        if (delivery != null && delivery.getAddress() != null) {
            Address a = delivery.getAddress();
            res.setAddress(
                    a.getHouseNo() + ", " + a.getStreet() + ", " + a.getCity() +
                            (a.getDistrict() != null ? ", " + a.getDistrict() : "")
            );
        } else {
            res.setAddress("No address found");
        }
        return res;
    }

    @Transactional
    public void updateOrderStatus(Integer orderId, String newStatus) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        orderRepo.save(order);
    }

    @Transactional
    public void refundOrder(Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Payment payment = paymentRepo.findFirstByOrder_OrderIdOrderByTransactionDateDesc(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        // Prevent double refund
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("This order has already been refunded");
        }

        // Update payment status to REFUNDED
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepo.save(payment);

        System.out.println("✅ Refund issued for Order #" + orderId);

        // Send refund email
        try {
            emailService.sendRefundEmail(order.getUser(), order, payment);
            System.out.println("✅ Refund notification email sent to " + order.getUser().getEmail());
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send refund email: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelOrder(User user, Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("This order does not belong to you");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(order);

        try {
            emailService.sendOrderCancellationEmail(user, order);
        } catch (Exception e) {
            System.err.println("Failed to send cancellation email: " + e.getMessage());
        }
    }

    public List<Payment> getUserPayments(User user) {
        return paymentRepo.findByOrder_User_UserId(user.getUserId());
    }

    // ====================== MONTHLY SALES REPORT - FIXED ======================
    public byte[] generateMonthlySalesReport() {
        List<Order> allOrders = orderRepo.findAllByOrderByCreatedAtDesc();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            document.add(new com.itextpdf.layout.element.Paragraph("ANURA FURNITURES")
                    .setBold().setFontSize(24).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("MONTHLY SALES REPORT")
                    .setBold().setFontSize(18).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("Generated on: " +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a")))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            BigDecimal totalSales = BigDecimal.ZERO;
            int totalOrders = 0;
            int successfulOrders = 0;

            for (Order order : allOrders) {
                // FIXED: Exclude refunded orders as well
                Payment payment = paymentRepo.findFirstByOrder_OrderIdOrderByTransactionDateDesc(order.getOrderId())
                        .orElse(null);
                boolean isRefunded = payment != null && payment.getStatus() == PaymentStatus.REFUNDED;

                if (order.getStatus() != OrderStatus.CANCELLED && !isRefunded) {
                    totalSales = totalSales.add(order.getTotalAmount());
                    totalOrders++;
                    if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
                        successfulOrders++;
                    }
                }
            }

            document.add(new com.itextpdf.layout.element.Paragraph("SUMMARY")
                    .setBold().setFontSize(16));
            document.add(new com.itextpdf.layout.element.Paragraph("Total Orders (excluding cancelled & refunded): " + totalOrders));
            document.add(new com.itextpdf.layout.element.Paragraph("Successful/Delivered Orders: " + successfulOrders));
            document.add(new com.itextpdf.layout.element.Paragraph("Total Revenue: Rs. " + totalSales)
                    .setBold().setFontSize(16));
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            // Detailed table
            document.add(new com.itextpdf.layout.element.Paragraph("DETAILED ORDERS")
                    .setBold().setFontSize(14));
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(5);
            table.addHeaderCell("Order ID");
            table.addHeaderCell("Customer");
            table.addHeaderCell("Amount");
            table.addHeaderCell("Status");
            table.addHeaderCell("Date");

            for (Order order : allOrders) {
                Payment payment = paymentRepo.findFirstByOrder_OrderIdOrderByTransactionDateDesc(order.getOrderId())
                        .orElse(null);
                boolean isRefunded = payment != null && payment.getStatus() == PaymentStatus.REFUNDED;

                if (order.getStatus() != OrderStatus.CANCELLED && !isRefunded) {
                    String customerName = order.getUser().getFirstName() +
                            (order.getUser().getLastName() != null ? " " + order.getUser().getLastName() : "");

                    table.addCell("#" + order.getOrderId());
                    table.addCell(customerName);
                    table.addCell("Rs. " + order.getTotalAmount());
                    table.addCell(order.getStatus().name());
                    table.addCell(order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                }
            }

            document.add(table);
            document.add(new com.itextpdf.layout.element.Paragraph("\nThank you for using Anura Furnitures Admin Panel.")
                    .setItalic());

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    // ====================== DELIVERY MANAGEMENT ======================
    public List<DeliveryResponse> getAllDeliveriesForAdmin() {
        // FIXED: Only show deliveries that have been assigned (have partner + tracking code)
        List<Delivery> deliveries = deliveryRepo.findAll().stream()
                .filter(d -> d.getDeliveryPartner() != null && !d.getDeliveryPartner().isEmpty())
                .toList();

        return deliveries.stream().map(this::mapToDeliveryResponse).collect(Collectors.toList());
    }

    public List<DeliveryResponse> getReadyToAssignDeliveries() {
        // Confirmed orders that still have PENDING delivery (no partner assigned yet)
        List<Delivery> pendings = deliveryRepo.findByStatus(DeliveryStatus.PENDING);
        return pendings.stream()
                .filter(d -> d.getOrder().getStatus() == OrderStatus.CONFIRMED)
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    private DeliveryResponse mapToDeliveryResponse(Delivery d) {
        DeliveryResponse res = new DeliveryResponse();
        res.setDeliveryId(d.getDeliveryId());
        res.setOrderId(d.getOrder().getOrderId());
        res.setCustomerName(d.getOrder().getUser().getFirstName() + " " +
                (d.getOrder().getUser().getLastName() != null ? d.getOrder().getUser().getLastName() : ""));
        res.setCustomerEmail(d.getOrder().getUser().getEmail());
        res.setTrackingCode(d.getTrackingCode());
        res.setDeliveryPartner(d.getDeliveryPartner());
        res.setStatus(d.getStatus().name());
        res.setDeliveryDate(d.getDeliveryDate());

        Address a = d.getAddress();
        res.setAddressSummary(
                (a.getHouseNo() != null ? a.getHouseNo() + ", " : "") +
                        (a.getStreet() != null ? a.getStreet() + ", " : "") +
                        (a.getCity() != null ? a.getCity() : "") +
                        (a.getDistrict() != null ? ", " + a.getDistrict() : "")
        );
        return res;
    }

    @Transactional
    public void assignDelivery(Integer orderId, String deliveryPartner, String deliveryDateStr) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Delivery delivery = deliveryRepo.findByOrder_OrderId(orderId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No delivery record found for this order"));

        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Delivery already assigned");
        }

        delivery.setDeliveryPartner(deliveryPartner);

        if (deliveryDateStr != null && !deliveryDateStr.isEmpty()) {
            // ── NEW VALIDATION ── Future date only
            LocalDateTime deliveryDate = LocalDateTime.parse(deliveryDateStr + "T00:00:00");
            if (deliveryDate.toLocalDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Delivery date must be today or in the future. Past dates are not allowed.");
            }
            delivery.setDeliveryDate(deliveryDate);
        }

        if (delivery.getTrackingCode() == null || delivery.getTrackingCode().isEmpty()) {
            delivery.setTrackingCode("ANURA-DEL-" + String.format("%08d", System.currentTimeMillis() % 100000000));
        }

        deliveryRepo.save(delivery);

        // SEND EMAIL WITH TRACKING CODE
        emailService.sendDeliveryCreatedEmail(order.getUser(), order, delivery);
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

        emailService.sendDeliveryCancelledEmail(delivery.getOrder().getUser(), delivery.getOrder(), delivery);
    }
    /**
     * Reschedule an existing delivery (for delays or customer requests)
     */
    @Transactional
    public void rescheduleDelivery(Integer deliveryId, String newDeliveryDateStr) {
        Delivery delivery = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

        // Cannot reschedule completed or cancelled deliveries
        if (delivery.getStatus() == DeliveryStatus.DELIVERED || delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reschedule a delivered or cancelled delivery");
        }

        // ── FUTURE DATE VALIDATION ──
        LocalDateTime newDate = LocalDateTime.parse(newDeliveryDateStr + "T00:00:00");
        if (newDate.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Delivery date must be today or in the future. Past dates are not allowed.");
        }

        delivery.setDeliveryDate(newDate);
        deliveryRepo.save(delivery);

        // Notify customer
        emailService.sendDeliveryRescheduledEmail(
                delivery.getOrder().getUser(),
                delivery.getOrder(),
                delivery
        );

        System.out.println("✅ Delivery #" + deliveryId + " rescheduled to " + newDeliveryDateStr);
    }

    // Customer can see their own deliveries in Profile
    public List<DeliveryResponse> getMyDeliveries(User user) {
        List<Delivery> list = deliveryRepo.findByOrder_User_UserId(user.getUserId());
        return list.stream().map(this::mapToDeliveryResponse).collect(Collectors.toList());
    }

    // Public tracking page (no login needed)
    public DeliveryResponse trackByCode(String trackingCode) {
        Delivery d = deliveryRepo.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tracking code"));

        return mapToDeliveryResponse(d);
    }
}
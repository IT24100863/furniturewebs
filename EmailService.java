package com.webs.furniturewebs.service;

import com.webs.furniturewebs.entity.*;
import com.webs.furniturewebs.entity.SupportTicket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    // ====================== PASSWORD RESET (UNTOUCHED) ======================
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "FurnitureWeb - Password Reset Request";
        String resetUrl = "http://localhost:8080/reset-password.html?token=" + resetToken;

        String body = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Password Reset Request</h2>
                    <p>Hello,</p>
                    <p>You requested a password reset for your FurnitureWeb account.</p>
                    <p>Click the link below to reset your password:</p>
                    <p style="margin: 20px 0;">
                        <a href="%s" style="background: #e67e22; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">
                            Reset My Password
                        </a>
                    </p>
                    <p>This link will expire in 2 hours.</p>
                    <p>If you did not request this, please ignore this email.</p>
                    <p>Best regards,<br>FurnitureWeb Team</p>
                </body>
                </html>
                """.formatted(resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            System.out.println("Reset email sent successfully to: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Failed to send reset email to " + toEmail);
            e.printStackTrace();
        }
    }

    // ====================== ORDER CONFIRMATION ======================
    public void sendOrderConfirmation(User user, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Order Confirmation #" + order.getOrderId() + " - Anura Furnitures");

            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("order", order);
            context.setVariable("items", order.getItems());
            context.setVariable("total", order.getTotalAmount());
            context.setVariable("date", order.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a")));

            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/order-confirmation", context);
                System.out.println("DEBUG: Thymeleaf template processed successfully");
            } catch (Exception e) {
                System.err.println("Thymeleaf template failed, using fallback HTML");
                htmlContent = getFallbackOrderConfirmation(user, order);
            }

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("✅ Order confirmation email sent to: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("Failed to send order confirmation to " + user.getEmail());
            e.printStackTrace();
        }
    }

    // Fallback HTML in case template has issues
    private String getFallbackOrderConfirmation(User user, Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<li>").append(item.getQuantity())
                    .append(" × ").append(item.getProduct().getName())
                    .append(" @ Rs. ").append(item.getPrice())
                    .append("</li>");
        }

        return """
            <html>
            <body style="font-family: Arial, sans-serif; background:#f9f9fa; padding:20px;">
            <div style="max-width:600px; margin:auto; background:white; padding:30px; border-radius:10px;">
                <h2 style="color:#e67e22;">Thank You for Your Order!</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your order has been successfully placed.</p>
                <p><strong>Order ID:</strong> %d</p>
                <ul>%s</ul>
                <h3 style="color:#e67e22;">Total: Rs. %s</h3>
                <p>Thank you for shopping at Anura Furnitures!</p>
            </div>
            </body>
            </html>
            """.formatted(user.getFirstName(), order.getOrderId(), itemsHtml, order.getTotalAmount());
    }

    // ====================== PAYMENT VERIFICATION ======================
    public void sendPaymentVerificationEmail(User user, Order order, Payment payment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Payment Verified - Order #" + order.getOrderId() + " - Anura Furnitures");

            String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2 style="color: #2c3e50;">Payment Verification Successful</h2>
                    <p>Dear %s,</p>
                    <p>Your payment for Order <strong>#%d</strong> has been successfully verified.</p>
                    <ul>
                        <li><strong>Order ID:</strong> %d</li>
                        <li><strong>Payment Method:</strong> %s</li>
                        <li><strong>Amount:</strong> Rs. %s</li>
                        <li><strong>Status:</strong> %s</li>
                    </ul>
                    <p>Thank you for shopping with Anura Furnitures!</p>
                </body>
                </html>
                """.formatted(user.getFirstName(), order.getOrderId(), order.getOrderId(),
                    payment.getPaymentMethod(), payment.getAmount(), payment.getStatus().name());

            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("✅ Payment verification email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send payment verification email");
            e.printStackTrace();
        }
    }

    // ====================== INVOICE EMAIL ======================
    public void sendInvoiceEmail(User user, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Invoice for Order #" + order.getOrderId() + " - Anura Furnitures");

            String htmlBody = """
                <h2>Thank you for your purchase!</h2>
                <p>Dear %s,</p>
                <p>Please find attached the official invoice for your recent order.</p>
                <p><strong>Order ID:</strong> %d<br>
                <strong>Total Amount:</strong> Rs. %s</p>
                <p>Best regards,<br><strong>Anura Furnitures Team</strong></p>
                """.formatted(user.getFirstName(), order.getOrderId(), order.getTotalAmount());

            helper.setText(htmlBody, true);

            byte[] pdfBytes = generateSimpleInvoicePdf(order, user);
            helper.addAttachment("Invoice_Order_" + order.getOrderId() + ".pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("✅ Invoice email with PDF sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send invoice email");
            e.printStackTrace();
        }
    }

    // ====================== CANCELLATION EMAIL ======================
    public void sendOrderCancellationEmail(User user, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Order Cancelled #" + order.getOrderId() + " - Anura Furnitures");

            String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                    <h2 style="color: #e74c3c;">Order Cancellation Confirmation</h2>
                    <p>Dear %s,</p>
                    <p>Your order <strong>#%d</strong> has been successfully cancelled.</p>
                    <p>Best regards,<br><strong>Anura Furnitures Team</strong></p>
                </body>
                </html>
                """.formatted(user.getFirstName(), order.getOrderId());

            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("✅ Cancellation email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send cancellation email");
        }
    }

    // ====================== NEW: REFUND EMAIL ======================
    /**
     * Send Refund Notification Email to Customer
     */
    public void sendRefundEmail(User user, Order order, Payment payment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Refund Processed - Order #" + order.getOrderId() + " - Anura Furnitures");

            String htmlBody = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #27ae60;">Refund Confirmation</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your refund for Order <strong>#%d</strong> has been successfully processed.</p>
                    
                    <div style="background:#f8f9fa; padding:15px; border-radius:8px; margin:20px 0;">
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Refund Amount:</strong> Rs. %s</p>
                        <p><strong>Payment Method:</strong> %s</p>
                        <p><strong>Refund Status:</strong> %s</p>
                    </div>
                    
                    <p>The refunded amount will be credited back to your original payment method within 5-7 business days.</p>
                    <p>If you have any questions, feel free to contact us.</p>
                    
                    <p>Thank you for shopping with <strong>Anura Furnitures</strong>!</p>
                    <p>Best regards,<br><strong>Anura Furnitures Team</strong></p>
                </body>
                </html>
                """.formatted(
                    user.getFirstName(),
                    order.getOrderId(),
                    order.getOrderId(),
                    payment.getAmount(),
                    payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A",
                    payment.getStatus().name()
            );

            helper.setText(htmlBody, true);
            mailSender.send(message);

            System.out.println("✅ Refund email sent successfully to: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("Failed to send refund email to " + user.getEmail());
            e.printStackTrace();
        }
    }

    private byte[] generateSimpleInvoicePdf(Order order, User user) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

        document.add(new com.itextpdf.layout.element.Paragraph("ANURA FURNITURES")
                .setBold().setFontSize(22));

        document.add(new com.itextpdf.layout.element.Paragraph("Invoice #" + order.getOrderId())
                .setFontSize(16));

        document.add(new com.itextpdf.layout.element.Paragraph("Date: " +
                order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));

        document.add(new com.itextpdf.layout.element.Paragraph("Customer: " +
                user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")));

        document.add(new com.itextpdf.layout.element.Paragraph("\nOrder Items:").setBold());

        for (OrderItem item : order.getItems()) {
            BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            document.add(new com.itextpdf.layout.element.Paragraph(
                    item.getQuantity() + " × " + item.getProduct().getName() +
                            " @ Rs. " + item.getPrice() + " = Rs. " + subtotal
            ));
        }

        document.add(new com.itextpdf.layout.element.Paragraph("\nGrand Total: Rs. " + order.getTotalAmount())
                .setBold().setFontSize(18));

        document.add(new com.itextpdf.layout.element.Paragraph("\nThank you for shopping with us!"));

        document.close();
        return baos.toByteArray();
    }

    public void sendDeliveryCreatedEmail(User user, Order order, Delivery delivery) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("🚚 Your Delivery Has Been Scheduled – Order #" + order.getOrderId());

            String html = """
                <h2 style="color:#e67e22;">Delivery Scheduled!</h2>
                <p>Dear %s,</p>
                <p>Your order <strong>#%d</strong> has been assigned to a delivery partner.</p>
                <ul>
                    <li><strong>Tracking Code:</strong> <b>%s</b></li>
                    <li><strong>Partner:</strong> %s</li>
                    <li><strong>Estimated Date:</strong> %s</li>
                </ul>
                <p>Use the tracking code above on our website to check status.</p>
                <p>Thank you for shopping at Anura Furnitures!</p>
                """.formatted(user.getFirstName(), order.getOrderId(),
                    delivery.getTrackingCode(), delivery.getDeliveryPartner(),
                    delivery.getDeliveryDate() != null ? delivery.getDeliveryDate().toLocalDate() : "TBD");

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send delivery created email");
        }
    }

    public void sendDeliveryCancelledEmail(User user, Order order, Delivery delivery) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("❌ Delivery Cancelled – Order #" + order.getOrderId());

            String html = """
                <h2 style="color:#e74c3c;">Delivery Cancelled</h2>
                <p>Dear %s,</p>
                <p>Your delivery for Order <strong>#%d</strong> has been cancelled.</p>
                <p>We are sorry for any inconvenience. Your order status has been updated.</p>
                <p>Thank you,<br>Anura Furnitures Team</p>
                """.formatted(user.getFirstName(), order.getOrderId());

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send delivery cancelled email");
        }
    }
    // ====================== DELIVERY RESCHEDULE NOTIFICATION ======================
    public void sendDeliveryRescheduledEmail(User user, Order order, Delivery delivery) {
        String subject = "Anura Furnitures - Your Delivery Date Has Been Rescheduled";

        String formattedDate = delivery.getDeliveryDate() != null
                ? delivery.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "TBD";

        String body = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <h2 style="color: #e67e22;">📅 Delivery Date Updated</h2>
                <p>Dear %s,</p>
                <p>The scheduled delivery date for your Order <strong>#%d</strong> has been changed.</p>
                <p><strong>New Delivery Date:</strong> %s</p>
                <p><strong>Tracking Code:</strong> %s</p>
                <p><strong>Delivery Partner:</strong> %s</p>
                <p>We apologise for any inconvenience caused. If you have any questions, please contact our support team.</p>
                <p>Thank you for your patience,<br><strong>Anura Furnitures Team</strong></p>
            </body>
            </html>
            """.formatted(
                user.getFirstName() != null ? user.getFirstName() : "Customer",
                order.getOrderId(),
                formattedDate,
                delivery.getTrackingCode() != null ? delivery.getTrackingCode() : "N/A",
                delivery.getDeliveryPartner() != null ? delivery.getDeliveryPartner() : "TBD"
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            System.out.println("✅ Delivery reschedule email sent to: " + user.getEmail());
        } catch (MessagingException e) {
            System.err.println("Failed to send delivery reschedule email: " + e.getMessage());
        }
    }
    // ====================== NEW: SUPPORT TICKET CREATED EMAIL ======================
    /**
     * Sends email to customer when a support ticket is created.
     * Includes ticket code and tracking link.
     */
    public void sendTicketCreatedEmail(User user, SupportTicket ticket) {
        String subject = "Anura Furnitures - Support Ticket Received #" + ticket.getTicketCode();

        String trackingUrl = "http://localhost:8080/track-ticket.html?code=" + ticket.getTicketCode();

        String htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background:#f9f9f9; padding:20px; }
                    .container { max-width: 600px; margin: auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: #5D4037; color: white; padding: 25px; text-align: center; }
                    .content { padding: 30px; line-height: 1.7; color: #333; }
                    .ticket-code { 
                        background: #f5f5dc; 
                        padding: 15px; 
                        border-radius: 8px; 
                        font-size: 1.4rem; 
                        font-weight: bold; 
                        text-align: center; 
                        color: #5D4037; 
                        margin: 20px 0;
                    }
                    .btn {
                        display: inline-block;
                        background: #e67e22;
                        color: white;
                        padding: 14px 28px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        margin: 20px 0;
                    }
                    .footer { 
                        text-align: center; 
                        padding: 20px; 
                        font-size: 0.9rem; 
                        color: #777; 
                        border-top: 1px solid #eee;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎟️ Support Ticket Received</h1>
                    </div>
                    <div class="content">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Thank you for contacting Anura Furnitures Support.</p>
                        
                        <p>Your ticket has been successfully created and our team will respond as soon as possible.</p>
                        
                        <div class="ticket-code">
                            Ticket Code: <span style="color:#e67e22;">%s</span>
                        </div>
                        
                        <p><strong>Subject:</strong> %s</p>
                        <p><strong>Request Type:</strong> %s</p>
                        
                        <p style="text-align:center;">
                            <a href="%s" class="btn">Track Your Ticket Status</a>
                        </p>
                        
                        <p>You can use the above link or search the ticket code on our website anytime to check updates and replies.</p>
                    </div>
                    <div class="footer">
                        <p>Best regards,<br><strong>Anura Furnitures Team</strong></p>
                        <p>If you did not create this ticket, please ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                user.getFirstName() != null ? user.getFirstName() : "Customer",
                ticket.getTicketCode(),
                ticket.getSubject(),
                ticket.getRequestType() != null ? ticket.getRequestType() : "General",
                trackingUrl
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("✅ Support ticket email sent to: " + user.getEmail() + " | Code: " + ticket.getTicketCode());

        } catch (MessagingException e) {
            System.err.println("Failed to send ticket creation email to " + user.getEmail());
            e.printStackTrace();
        }
    }



}
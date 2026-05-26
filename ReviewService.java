package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.ReviewAdminResponse;
import com.webs.furniturewebs.dto.ReviewRequest;
import com.webs.furniturewebs.entity.Product;
import com.webs.furniturewebs.entity.Review;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.repository.ProductRepository;
import com.webs.furniturewebs.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import com.webs.furniturewebs.dto.ReviewResponseForProfile;
import com.webs.furniturewebs.dto.ReviewUpdateRequest;
import com.webs.furniturewebs.entity.ProductImage;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;

    public ReviewService(ReviewRepository reviewRepo, ProductRepository productRepo) {
        this.reviewRepo = reviewRepo;
        this.productRepo = productRepo;
    }

    public void addReview(User user, ReviewRequest req) {
        Product product = productRepo.findById(req.getProductId()).orElseThrow();

        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(req.getRating());
        review.setComment(req.getComment());

        reviewRepo.save(review);
    }
    // ==================== NEW METHODS ====================

    public List<ReviewResponseForProfile> getMyReviews(User user) {
        return reviewRepo.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponseForProfile mapToProfileResponse(Review review) {
        ReviewResponseForProfile res = new ReviewResponseForProfile();
        res.setReviewId(review.getReviewId());
        res.setProductId(review.getProduct().getProductId());
        res.setProductName(review.getProduct().getName());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setCreatedAt(review.getCreatedAt());

        // Get first image
        if (!review.getProduct().getImages().isEmpty()) {
            res.setProductImageUrl(review.getProduct().getImages().get(0).getImageUrl());
        }

        return res;
    }

    @Transactional
    public void updateReview(User user, Integer reviewId, ReviewUpdateRequest req) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("You can only edit your own reviews");
        }

        review.setRating(req.getRating());
        review.setComment(req.getComment());
        reviewRepo.save(review);
    }

    @Transactional
    public void deleteReview(User user, Integer reviewId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("You can only delete your own reviews");
        }

        reviewRepo.delete(review);
    }
    // ==================== ADMIN METHODS ====================

    public List<ReviewAdminResponse> getAllReviewsForAdmin() {
        return reviewRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    private ReviewAdminResponse mapToAdminResponse(Review r) {
        ReviewAdminResponse res = new ReviewAdminResponse();
        res.setReviewId(r.getReviewId());
        res.setProductName(r.getProduct().getName());
        res.setUserName(r.getUser().getFirstName() +
                (r.getUser().getLastName() != null ? " " + r.getUser().getLastName() : ""));
        res.setRating(r.getRating());
        res.setComment(r.getComment());
        res.setCreatedAt(r.getCreatedAt());
        return res;
    }

    @Transactional
    public void adminUpdateReview(Integer reviewId, ReviewUpdateRequest req) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setRating(req.getRating());
        review.setComment(req.getComment());
        reviewRepo.save(review);
    }

    @Transactional
    public void adminDeleteReview(Integer reviewId) {
        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        reviewRepo.delete(review);
    }

    /**
     * NEW: Generate Feedback Report as PDF (same style as monthly sales report)
     */
    public byte[] generateFeedbackReport() {
        List<Review> reviews = reviewRepo.findAllByOrderByCreatedAtDesc();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            document.add(new com.itextpdf.layout.element.Paragraph("ANURA FURNITURES")
                    .setBold().setFontSize(24).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("CUSTOMER FEEDBACK REPORT")
                    .setBold().setFontSize(18).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("Generated on: " +
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, hh:mm a")))
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            int totalReviews = reviews.size();
            double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

            document.add(new com.itextpdf.layout.element.Paragraph("SUMMARY")
                    .setBold().setFontSize(16));
            document.add(new com.itextpdf.layout.element.Paragraph("Total Reviews: " + totalReviews));
            document.add(new com.itextpdf.layout.element.Paragraph("Average Rating: " + String.format("%.1f ★", avgRating)));

            // Rating breakdown
            java.util.Map<Integer, Long> counts = reviews.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Review::getRating, java.util.stream.Collectors.counting()));
            document.add(new com.itextpdf.layout.element.Paragraph("\nRating Breakdown:"));
            for (int i = 5; i >= 1; i--) {
                long cnt = counts.getOrDefault(i, 0L);
                document.add(new com.itextpdf.layout.element.Paragraph(i + " ★ : " + cnt + " reviews"));
            }

            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            // Detailed table
            document.add(new com.itextpdf.layout.element.Paragraph("ALL CUSTOMER REVIEWS")
                    .setBold().setFontSize(14));
            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(5);
            table.addHeaderCell("Review ID");
            table.addHeaderCell("Product");
            table.addHeaderCell("Customer");
            table.addHeaderCell("Rating");
            table.addHeaderCell("Comment");

            for (Review r : reviews) {
                String userName = r.getUser().getFirstName() +
                        (r.getUser().getLastName() != null ? " " + r.getUser().getLastName() : "");
                table.addCell(r.getReviewId().toString());
                table.addCell(r.getProduct().getName());
                table.addCell(userName);
                table.addCell("★".repeat(r.getRating()));
                table.addCell(r.getComment() != null && !r.getComment().isEmpty() ? r.getComment() : "(no comment)");
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
}
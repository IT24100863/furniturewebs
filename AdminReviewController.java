package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.ReviewAdminResponse;
import com.webs.furniturewebs.dto.ReviewUpdateRequest;
import com.webs.furniturewebs.service.ReviewService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewAdminResponse> getAll() {
        return reviewService.getAllReviewsForAdmin();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> update(@PathVariable Integer id,
                                         @RequestBody ReviewUpdateRequest req) {
        reviewService.adminUpdateReview(id, req);
        return ResponseEntity.ok("Review updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        reviewService.adminDeleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports/feedback")
    public ResponseEntity<byte[]> generateFeedbackReport() {
        byte[] pdfBytes = reviewService.generateFeedbackReport();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "Anura_Furnitures_Feedback_Report_" +
                        java.time.LocalDate.now().toString() + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
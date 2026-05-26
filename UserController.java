package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.*;
import com.webs.furniturewebs.entity.LoginHistory;
import com.webs.furniturewebs.entity.Payment;
import com.webs.furniturewebs.entity.SavedCard;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.repository.LoginHistoryRepository;
import com.webs.furniturewebs.repository.SavedCardRepository;
import com.webs.furniturewebs.repository.UserRepository;
import com.webs.furniturewebs.service.OrderService;
import com.webs.furniturewebs.service.UserService;
import com.webs.furniturewebs.dto.DeliveryResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final SavedCardRepository savedCardRepo;
    private final OrderService orderService;

    // Updated Constructor
    public UserController(UserService userService,
                          LoginHistoryRepository loginHistoryRepository,
                          UserRepository userRepository,
                          SavedCardRepository savedCardRepo,
                          OrderService orderService) {
        this.userService = userService;
        this.loginHistoryRepository = loginHistoryRepository;
        this.userRepository = userRepository;
        this.savedCardRepo = savedCardRepo;
        this.orderService = orderService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getProfile(user));
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponse> updateProfile(@AuthenticationPrincipal User user,
                                                      @RequestBody UpdateProfileRequest req) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User updated = userService.updateProfile(user, req);
        UserResponse res = new UserResponse();
        res.setUserId(updated.getUserId());
        res.setFirstName(updated.getFirstName());
        res.setLastName(updated.getLastName());
        res.setEmail(updated.getEmail());
        res.setPhone(updated.getPhone());
        res.setRole(updated.getRole());
        res.setIsVerified(updated.getIsVerified());
        res.setPreferredLanguage(updated.getPreferredLanguage());
        res.setLanguageSelected(updated.getLanguageSelected());
        res.setAuthProvider(updated.getAuthProvider().name());

        return ResponseEntity.ok(res);
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginHistory>> getLoginHistory(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<LoginHistory> history = loginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/deactivate")
    @Transactional
    public ResponseEntity<String> deactivateAccount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to perform this action");
        }

        if ("GOOGLE".equals(user.getAuthProvider().name()) || "ADMIN".equals(user.getRole().name())) {
            return ResponseEntity.badRequest()
                    .body("This account type cannot be deactivated from here");
        }

        user.setStatus("DEACTIVATED");
        userRepository.save(user);

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("Your account has been deactivated successfully. "
                + "You have been logged out.");
    }

    // Set preferred language for first-time Google users
    @PostMapping("/set-language")
    public ResponseEntity<String> setPreferredLanguage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("You must be logged in to perform this action");
        }

        String lang = body.get("preferredLanguage");
        if (lang == null || lang.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Language is required");
        }

        user.setPreferredLanguage(lang.trim());
        user.setLanguageSelected(true);
        userRepository.save(user);

        return ResponseEntity.ok("Language preference saved successfully");
    }

    // ====================== SAVED CARDS ======================

    /**
     * Get all saved cards (used in Profile page)
     */
    @GetMapping("/saved-cards")
    public ResponseEntity<List<SavedCardResponse>> getSavedCards(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SavedCard> cards = savedCardRepo.findByUser_UserId(user.getUserId());

        List<SavedCardResponse> res = cards.stream().map(this::mapToSavedCardResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    /**
     * NEW ENDPOINT: Get saved cards specifically for Checkout page
     * This includes maskedNumber for better UX
     */
    @GetMapping("/saved-cards/checkout")
    public ResponseEntity<List<SavedCardResponse>> getSavedCardsForCheckout(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<SavedCard> cards = savedCardRepo.findByUser_UserId(user.getUserId());

        List<SavedCardResponse> res = cards.stream().map(this::mapToSavedCardResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }

    /**
     * Delete a saved card
     */
    @DeleteMapping("/saved-cards/{id}")
    public ResponseEntity<Void> deleteSavedCard(@AuthenticationPrincipal User user,
                                                @PathVariable Integer id) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SavedCard card = savedCardRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (!card.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("Not your card");
        }

        savedCardRepo.delete(card);
        return ResponseEntity.noContent().build();
    }

    // ====================== TRANSACTIONS ======================
    @GetMapping("/transactions")
    public ResponseEntity<List<PaymentResponse>> getTransactions(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Payment> payments = orderService.getUserPayments(user);

        List<PaymentResponse> res = payments.stream().map(p -> {
            PaymentResponse r = new PaymentResponse();
            r.setPaymentId(p.getPaymentId());
            r.setOrderId(p.getOrder().getOrderId());
            r.setAmount(p.getAmount());
            r.setStatus(p.getStatus().name());
            r.setPaymentMethod(p.getPaymentMethod());
            r.setTransactionDate(p.getTransactionDate());
            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(res);
    }
    // ====================== MY DELIVERIES (for customer profile) ======================
    // ====================== MY DELIVERIES (Customer Profile) ======================
    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryResponse>> getMyDeliveries(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<DeliveryResponse> deliveries = orderService.getMyDeliveries(user);
        return ResponseEntity.ok(deliveries);
    }

    // ====================== HELPER MAPPER ======================
    private SavedCardResponse mapToSavedCardResponse(SavedCard c) {
        SavedCardResponse r = new SavedCardResponse();
        r.setCardId(c.getCardId());
        r.setLast4(c.getLast4());
        r.setMaskedNumber(c.getMaskedNumber() != null
                ? c.getMaskedNumber()
                : "**** **** **** " + (c.getLast4() != null ? c.getLast4() : "****"));
        r.setCardHolderName(c.getCardHolderName());
        r.setExpiryDate(c.getExpiryDate());
        r.setCardType(c.getCardType());
        return r;
    }
}
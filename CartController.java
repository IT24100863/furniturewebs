package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.AddToCartRequest;
import com.webs.furniturewebs.dto.CartResponse;
import com.webs.furniturewebs.dto.UpdateCartRequest;          // ← new DTO you should create
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for shopping cart operations
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get current user's cart contents (items + total)
     */
    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal User user) {
        // Note: if user is null → frontend should redirect to login
        return cartService.getCart(user);
    }

    /**
     * Add product to cart (or increase quantity if already present)
     */
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal User user,
            @RequestBody AddToCartRequest req) {

        cartService.addToCart(user, req.getProductId(), req.getQuantity());
        return ResponseEntity.ok("Item added to cart");
    }

    /**
     * Update quantity of a specific item in cart
     * - If quantity <= 0 → item will be removed
     */
    @PutMapping("/update")
    public ResponseEntity<String> updateCartItem(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateCartRequest req) {

        cartService.updateCartItem(user, req.getProductId(), req.getQuantity());
        return ResponseEntity.ok("Cart updated successfully");
    }

    /**
     * Remove a specific product from the cart
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<String> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Integer productId) {

        cartService.removeFromCart(user, productId);
        return ResponseEntity.ok("Item removed from cart");
    }

    /**
     * Clear / empty the entire cart
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        return ResponseEntity.ok("Cart has been cleared");
    }

    /**
     * Get only the total number of items in cart (useful for navbar badge)
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getCartItemCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.ok(0);   // No user = empty cart
        }
        try {
            int count = cartService.getCartItemCount(user);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.ok(0);
        }
    }
}
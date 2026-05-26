package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.CartResponse;
import com.webs.furniturewebs.dto.CartItemResponse;
import com.webs.furniturewebs.entity.Cart;
import com.webs.furniturewebs.entity.CartItem;
import com.webs.furniturewebs.entity.Product;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.entity.ProductImage;
import com.webs.furniturewebs.repository.CartRepository;
import com.webs.furniturewebs.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cart management service
 */
@Service
@Transactional
public class CartService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;

    public CartService(CartRepository cartRepo, ProductRepository productRepo) {
        this.cartRepo = cartRepo;
        this.productRepo = productRepo;
    }

    /**
     * Get existing cart or create a new one for the user
     */
    public Cart getOrCreateCart(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must be logged in to access cart");
        }

        return cartRepo.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepo.save(cart);
                });
    }

    /**
     * Add product to cart (increases quantity if already present)
     */
    public void addToCart(User user, Integer productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Cart cart = getOrCreateCart(user);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    cart.getItems().add(newItem);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + quantity);

        // Optional: you could add stock check here
        // if (item.getQuantity() > product.getStock()) { ... }

        cartRepo.save(cart);
    }

    /**
     * Update quantity of an item in cart
     * If newQuantity <= 0 → item is removed
     */
    public void updateCartItem(User user, Integer productId, Integer newQuantity) {
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart: " + productId));

        if (newQuantity == null || newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            // Optional: stock validation
            // if (newQuantity > item.getProduct().getStock()) { ... }

            item.setQuantity(newQuantity);
        }

        cartRepo.save(cart);
    }

    /**
     * Remove specific product from cart
     */
    public void removeFromCart(User user, Integer productId) {
        Cart cart = getOrCreateCart(user);
        boolean removed = cart.getItems().removeIf(i -> i.getProduct().getProductId().equals(productId));

        if (removed) {
            cartRepo.save(cart);
        }
    }

    /**
     * Remove all items from cart (clear cart)
     */
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        if (!cart.getItems().isEmpty()) {
            cart.getItems().clear();
            cartRepo.save(cart);
        }
    }

    /**
     * Get current cart contents as DTO (with calculated total)
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);

        CartResponse res = new CartResponse();
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            CartItemResponse ir = new CartItemResponse();
            ir.setProductId(item.getProduct().getProductId());
            ir.setName(item.getProduct().getName());
            ir.setPrice(item.getProduct().getPrice());
            ir.setQuantity(item.getQuantity());

            // Assuming you have ProductImage entity with imageUrl field
            ir.setImageUrls(item.getProduct().getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));

            items.add(ir);

            // total += price × quantity
            total = total.add(
                    item.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        res.setItems(items);
        res.setTotal(total);
        return res;
    }

    /**
     * Optional: Get total number of items (sum of quantities)
     * Useful for badge/counter in navbar
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(User user) {
        if (user == null) {
            return 0;
        }
        try {
            Cart cart = getOrCreateCart(user);
            return cart.getItems().stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        } catch (Exception e) {
            return 0;
        }
    }
}

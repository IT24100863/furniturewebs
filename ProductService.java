package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.*;
import com.webs.furniturewebs.entity.*;
import com.webs.furniturewebs.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ReviewRepository reviewRepo;
    private final CartRepository cartRepo;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo,
                          ReviewRepository reviewRepo, CartRepository cartRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.reviewRepo = reviewRepo;
        this.cartRepo = cartRepo;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFilteredProducts(
            String keyword, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice) {

        List<Product> products;

        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepo.findByNameContainingIgnoreCase(keyword.trim());
        } else {
            products = productRepo.findAll();
        }

        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null &&
                            p.getCategory().getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        if (minPrice != null || maxPrice != null) {
            BigDecimal min = (minPrice != null) ? minPrice : BigDecimal.ZERO;
            BigDecimal max = (maxPrice != null) ? maxPrice : new BigDecimal("99999999");
            products = products.stream()
                    .filter(p -> p.getPrice().compareTo(min) >= 0 &&
                            p.getPrice().compareTo(max) <= 0)
                    .collect(Collectors.toList());
        }

        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Integer id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        ProductDetailResponse detail = new ProductDetailResponse();
        detail.setProductId(p.getProductId());
        detail.setName(p.getName());
        detail.setDescription(p.getDescription());
        detail.setPrice(p.getPrice());
        detail.setStock(p.getStock());
        detail.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "");
        detail.setImageUrls(p.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList()));

        double avg = p.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        detail.setAverageRating(avg);

        // Reviews
        detail.setReviews(p.getReviews().stream().map(r -> {
            ReviewResponse rr = new ReviewResponse();
            rr.setUserName(r.getUser().getFirstName() +
                    (r.getUser().getLastName() != null ? " " + r.getUser().getLastName() : ""));
            rr.setRating(r.getRating());
            rr.setComment(r.getComment());
            rr.setCreatedAt(r.getCreatedAt().toString());
            return rr;
        }).collect(Collectors.toList()));

        // Extra fields for detail view
        detail.setMaterial(p.getMaterial());
        detail.setColor(p.getColor());
        detail.setDimensions(p.getDimensions());
        detail.setWeight(p.getWeight());

        return detail;
    }

    // ────────────────────────────────────────────────
    //               ADMIN METHODS (YOUR ORIGINAL CODE)
// Inside ProductService.java → saveProduct method

    public Product saveProduct(AdminProductRequest req, MultipartFile[] images, Integer productId) {
        Product product;

        if (productId != null) {
            // UPDATE existing product
            product = productRepo.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        } else {
            // CREATE new product
            product = new Product();
        }

        // ==================== SAFE FIELD UPDATING ====================

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            product.setName(req.getName().trim());
        }

        if (req.getDescription() != null) {
            product.setDescription(req.getDescription());
        }

        // Price - Only require for NEW products. For updates, keep existing if not sent
        if (req.getPrice() != null) {
            product.setPrice(req.getPrice());
        } else if (productId == null) {   // Only throw if it's a NEW product
            throw new IllegalArgumentException("Price is required when creating a new product");
        }
        // If updating and price is null → keep the old price (don't change it)

        if (req.getStock() != null) {
            product.setStock(req.getStock());
        } else if (productId == null) {
            product.setStock(0);
        }

        if (req.getMaterial() != null) {
            product.setMaterial(req.getMaterial());
        }
        if (req.getColor() != null) {
            product.setColor(req.getColor());
        }
        if (req.getDimensions() != null) {
            product.setDimensions(req.getDimensions());
        }

        // Category - only update if provided
        if (req.getCategoryId() != null) {
            Category category = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }

        // Save product first
        product = productRepo.save(product);

        // ===================== IMAGE HANDLING =====================
        if (images != null && images.length > 0) {
            String uploadPath = uploadDir + "/products/";
            new File(uploadPath).mkdirs();

            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    try {
                        String fileName = product.getProductId() + "_"
                                + System.currentTimeMillis() + "_"
                                + file.getOriginalFilename();

                        Path path = Paths.get(uploadPath + fileName);
                        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                        ProductImage pi = new ProductImage();
                        pi.setProduct(product);
                        pi.setImageUrl("/uploads/products/" + fileName);

                        product.getImages().add(pi);
                    } catch (Exception e) {
                        System.err.println("Image upload failed: " + e.getMessage());
                    }
                }
            }
            product = productRepo.save(product);
        }

        return product;
    }

    public void deleteProduct(Integer id) {
        productRepo.deleteById(id);
    }

    // ────────────────────────────────────────────────
    //               MAPPER METHOD
    // ────────────────────────────────────────────────
    private ProductResponse mapToResponse(Product p) {
        ProductResponse r = new ProductResponse();
        r.setProductId(p.getProductId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setPrice(p.getPrice());
        r.setStock(p.getStock());
        r.setCategoryName(p.getCategory() != null ? p.getCategory().getName() : "");
        r.setImageUrls(p.getImages().stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toList()));

        double avg = p.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        r.setAverageRating(avg);

        r.setMaterial(p.getMaterial());
        r.setColor(p.getColor());
        r.setDimensions(p.getDimensions());
        r.setWeight(p.getWeight());

        return r;
    }
}
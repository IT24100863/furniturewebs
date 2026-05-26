package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.ProductResponse;
import com.webs.furniturewebs.entity.Offer;
import com.webs.furniturewebs.service.OfferService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    public List<Offer> getActiveOffers() {
        return offerService.getActiveOffers();
    }
    // In OfferController.java
    @GetMapping("/products/{offerId}")
    public List<ProductResponse> getProductsByOffer(@PathVariable Integer offerId) {
        // You need to implement this in OfferService
        return offerService.getProductsByOffer(offerId);
    }
}
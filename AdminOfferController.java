package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.OfferRequest;
import com.webs.furniturewebs.entity.Offer;
import com.webs.furniturewebs.service.OfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/offers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOfferController {

    private final OfferService offerService;

    public AdminOfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    public List<Offer> getAll() {
        return offerService.getAllOffers();
    }

    @PostMapping
    public ResponseEntity<Offer> create(@RequestBody OfferRequest req) {
        return ResponseEntity.ok(offerService.saveOffer(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{offerId}/assign")
    public ResponseEntity<String> assign(@PathVariable Integer offerId, @RequestBody Integer productId) {
        offerService.assignProductToOffer(offerId, productId);
        return ResponseEntity.ok("Product assigned to offer");
    }
}
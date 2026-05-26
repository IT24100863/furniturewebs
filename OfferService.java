package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.OfferRequest;
import com.webs.furniturewebs.dto.ProductResponse;
import com.webs.furniturewebs.entity.Offer;
import com.webs.furniturewebs.entity.Product;
import com.webs.furniturewebs.entity.ProductImage;
import com.webs.furniturewebs.entity.ProductOffer;
import com.webs.furniturewebs.repository.OfferRepository;
import com.webs.furniturewebs.repository.ProductOfferRepository;
import com.webs.furniturewebs.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OfferService {

    private final OfferRepository offerRepo;
    private final ProductOfferRepository productOfferRepo;
    private final ProductRepository productRepo;

    public OfferService(OfferRepository offerRepo, ProductOfferRepository productOfferRepo, ProductRepository productRepo) {
        this.offerRepo = offerRepo;
        this.productOfferRepo = productOfferRepo;
        this.productRepo = productRepo;
    }

    public List<Offer> getActiveOffers() {
        return offerRepo.findByIsActiveTrue();
    }

    public List<Offer> getAllOffers() {
        return offerRepo.findAll();
    }

    public Offer saveOffer(OfferRequest req) {
        Offer offer = new Offer();
        offer.setTitle(req.getTitle());
        offer.setDescription(req.getDescription());
        offer.setDiscountPercentage(req.getDiscountPercentage());
        offer.setStartDate(req.getStartDate());
        offer.setEndDate(req.getEndDate());
        offer.setIsActive(req.getIsActive() != null ? req.getIsActive() : true);
        return offerRepo.save(offer);
    }

    public void deleteOffer(Integer id) {
        offerRepo.deleteById(id);
    }

    public void assignProductToOffer(Integer offerId, Integer productId) {
        Offer offer = offerRepo.findById(offerId).orElseThrow();
        var product = productRepo.findById(productId).orElseThrow();

        ProductOffer po = new ProductOffer();
        po.setOffer(offer);
        po.setProduct(product);
        productOfferRepo.save(po);
    }

    public void removeProductFromOffer(Integer offerId, Integer productId) {
        productOfferRepo.findAll().stream()
                .filter(po -> po.getOffer().getOfferId().equals(offerId) &&
                        po.getProduct().getProductId().equals(productId))
                .findFirst().ifPresent(productOfferRepo::delete);
    }

    public List<ProductResponse> getProductsByOffer(Integer offerId) {
        List<ProductOffer> productOffers = productOfferRepo.findByOffer_OfferId(offerId);

        return productOffers.stream().map(po -> {
            Product p = po.getProduct();
            ProductResponse res = new ProductResponse();
            res.setProductId(p.getProductId());
            res.setName(p.getName());
            res.setPrice(p.getPrice());
            res.setImageUrls(p.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
            res.setDiscountPercentage(po.getOffer().getDiscountPercentage()); // add this field temporarily in DTO if needed
            return res;
        }).collect(Collectors.toList());
    }
}
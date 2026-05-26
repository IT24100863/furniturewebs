package com.webs.furniturewebs.repository;

import com.webs.furniturewebs.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Integer> {
    List<Offer> findByIsActiveTrue();
}
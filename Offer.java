package com.webs.furniturewebs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "offers")
@Getter @Setter @NoArgsConstructor
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer offerId;

    private String title;
    private String description;
    private Double discountPercentage;

    private LocalDate startDate;
    private LocalDate endDate;

    private Boolean isActive = true;
}


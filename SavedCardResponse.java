package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SavedCardResponse {
    private Integer cardId;
    private String last4;
    private String maskedNumber;      // ← Important
    private String cardHolderName;
    private String expiryDate;
    private String cardType;
}
package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlaceOrderRequest {

    // Address fields (used when creating new address)
    private Integer addressId;           // null = new address
    private String firstName;
    private String lastName;
    private String phone;
    private String country;
    private String province;
    private String district;
    private String city;
    private String street;
    private String houseNo;
    private String apartment;

    // Payment fields
    private String paymentMethod;        // CARD, GOOGLE_PAY, KOKO

    // Card details (only for CARD payment)
    private String cardNumber;
    private String cardHolderName;       // ← NEW
    private String expiry;               // ← NEW (MM/YY)
    private String cvv;
    private boolean saveCard;
}
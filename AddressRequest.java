package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressRequest {
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
}
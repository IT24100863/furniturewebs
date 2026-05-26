package com.webs.furniturewebs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressResponse {
    private Integer addressId;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    private String district;
    private String country;
    private String province;
    private String street;
    private String house_no;

    // add more if you want full display
}
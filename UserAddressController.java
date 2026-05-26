package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.AddressRequest;
import com.webs.furniturewebs.dto.AddressResponse;
import com.webs.furniturewebs.entity.Address;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    private final AddressService addressService;

    public UserAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(addressService.getUserAddresses(user));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@AuthenticationPrincipal User user,
                                                      @RequestBody AddressRequest req) {
        Address saved = addressService.saveAddress(user, req);
        AddressResponse res = new AddressResponse(); // map as before
        // ... copy fields (same as in getUserAddresses)
        res.setAddressId(saved.getAddressId());
        res.setFirstName(saved.getFirstName());
        // ... etc (or reuse the mapper from service)
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(@AuthenticationPrincipal User user,
                                                         @PathVariable Integer id,
                                                         @RequestBody AddressRequest req) {
        Address saved = addressService.updateAddress(id, user, req);
        // map to response same as above
        AddressResponse res = new AddressResponse();
        res.setAddressId(saved.getAddressId());
        // ... copy fields
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal User user,
                                              @PathVariable Integer id) {
        addressService.deleteAddress(id, user);
        return ResponseEntity.noContent().build();
    }
}
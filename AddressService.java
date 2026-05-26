package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.AddressRequest;
import com.webs.furniturewebs.dto.AddressResponse;
import com.webs.furniturewebs.dto.PlaceOrderRequest;
import com.webs.furniturewebs.entity.Address;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepo;

    public AddressService(AddressRepository addressRepo) {
        this.addressRepo = addressRepo;
    }

    public List<AddressResponse> getUserAddresses(User user) {
        return addressRepo.findByUser_UserId(user.getUserId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // For new Address management in Profile
    public Address saveAddress(User user, AddressRequest req) {
        Address addr = new Address();
        addr.setUser(user);
        updateAddressFields(addr, req);
        return addressRepo.save(addr);
    }

    // For backward compatibility with Checkout (PlaceOrderRequest)
    public Address saveAddress(User user, PlaceOrderRequest req) {
        Address addr = new Address();
        addr.setUser(user);
        addr.setFirstName(req.getFirstName());
        addr.setLastName(req.getLastName());
        addr.setPhone(req.getPhone());
        addr.setCountry(req.getCountry());
        addr.setProvince(req.getProvince());
        addr.setDistrict(req.getDistrict());
        addr.setCity(req.getCity());
        addr.setStreet(req.getStreet());
        addr.setHouseNo(req.getHouseNo());
        addr.setApartment(req.getApartment());
        return addressRepo.save(addr);
    }

    public Address updateAddress(Integer addressId, User user, AddressRequest req) {
        Address addr = addressRepo.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (!addr.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("Not your address");
        }

        updateAddressFields(addr, req);
        return addressRepo.save(addr);
    }

    public void deleteAddress(Integer addressId, User user) {
        Address addr = addressRepo.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (!addr.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalStateException("Not your address");
        }

        addressRepo.delete(addr);
    }

    // ==================== HELPER METHODS ====================

    private void updateAddressFields(Address addr, AddressRequest req) {
        addr.setFirstName(req.getFirstName());
        addr.setLastName(req.getLastName());
        addr.setPhone(req.getPhone());
        addr.setCountry(req.getCountry());
        addr.setProvince(req.getProvince());
        addr.setDistrict(req.getDistrict());
        addr.setCity(req.getCity());
        addr.setStreet(req.getStreet());
        addr.setHouseNo(req.getHouseNo());
        addr.setApartment(req.getApartment());
    }

    private AddressResponse mapToResponse(Address a) {
        AddressResponse r = new AddressResponse();
        r.setAddressId(a.getAddressId());
        r.setFirstName(a.getFirstName());
        r.setLastName(a.getLastName());
        r.setPhone(a.getPhone());
        r.setCity(a.getCity());
        r.setDistrict(a.getDistrict());
        r.setCountry(a.getCountry());
        r.setProvince(a.getProvince());
        r.setStreet(a.getStreet());
        r.setHouse_no(a.getHouseNo());
        return r;
    }
}
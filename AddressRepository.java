package com.webs.furniturewebs.repository;
import com.webs.furniturewebs.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUser_UserId(Integer userId);
}
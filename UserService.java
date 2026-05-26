package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.UpdateProfileRequest;
import com.webs.furniturewebs.dto.UserResponse;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UserResponse getProfile(User user) {
        UserResponse res = new UserResponse();
        res.setUserId(user.getUserId());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setRole(user.getRole());
        res.setIsVerified(user.getIsVerified());
        res.setPreferredLanguage(user.getPreferredLanguage());
        res.setLanguageSelected(user.getLanguageSelected());
        res.setAuthProvider(user.getAuthProvider().name());
        return res;
    }

    public User updateProfile(User user, UpdateProfileRequest req) {
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        return userRepo.save(user);
    }
}
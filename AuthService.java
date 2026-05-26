package com.webs.furniturewebs.service;

import com.webs.furniturewebs.dto.RegisterRequest;
import com.webs.furniturewebs.entity.AuthProvider;
import com.webs.furniturewebs.entity.LoginHistory;
import com.webs.furniturewebs.entity.PasswordReset;
import com.webs.furniturewebs.entity.Role;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.repository.LoginHistoryRepository;
import com.webs.furniturewebs.repository.PasswordResetRepository;
import com.webs.furniturewebs.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final LoginHistoryRepository loginHistoryRepo;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public AuthService(
            UserRepository userRepo,
            LoginHistoryRepository loginHistoryRepo,
            PasswordResetRepository passwordResetRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.loginHistoryRepo = loginHistoryRepo;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(Role.CUSTOMER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setIsVerified(true);
        user.setPreferredLanguage(req.getPreferredLanguage());
        user.setLanguageSelected(true);

        return userRepo.save(user);
    }

    public void saveLoginHistory(User user, HttpServletRequest request) {
        LoginHistory history = new LoginHistory();
        history.setUser(user);
        history.setIpAddress(request.getRemoteAddr());
        history.setDeviceInfo(request.getHeader("User-Agent"));
        loginHistoryRepo.save(history);
    }

    @Transactional
    public User findOrCreateGoogleUser(String email, String providerId, String firstName, String lastName) {
        return userRepo.findByProviderIdAndAuthProvider(providerId, AuthProvider.GOOGLE)
                .orElseGet(() -> userRepo.findByEmail(email)
                        .orElseGet(() -> {
                            User user = new User();
                            user.setFirstName(firstName);
                            user.setLastName(lastName);
                            user.setEmail(email);
                            user.setAuthProvider(AuthProvider.GOOGLE);
                            user.setProviderId(providerId);
                            user.setIsVerified(true);
                            user.setRole(Role.CUSTOMER);
                            user.setPreferredLanguage("en");
                            user.setLanguageSelected(false);   // First-time Google user needs to select language
                            return userRepo.save(user);
                        }));
    }

    // ────────────────────────────────────────────────
    //           PASSWORD RESET FUNCTIONALITY
    // ────────────────────────────────────────────────

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("No account found with this email"));

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new IllegalStateException("Google accounts cannot reset password here. Use Google account recovery.");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(2);

        PasswordReset reset = new PasswordReset();
        reset.setUser(user);
        reset.setToken(token);
        reset.setExpiryTime(expiry);

        passwordResetRepository.save(reset);

        // Send real email with reset link
        emailService.sendPasswordResetEmail(email, token);

        System.out.println("Reset token generated and email sent to: " + email);
        System.out.println("Token (debug only): " + token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordReset reset = passwordResetRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (reset.getExpiryTime().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(reset);
            throw new IllegalArgumentException("Reset token has expired");
        }

        User user = reset.getUser();
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Clean up used token
        passwordResetRepository.delete(reset);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }
}
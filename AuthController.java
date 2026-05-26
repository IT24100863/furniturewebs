package com.webs.furniturewebs.controller;

import com.webs.furniturewebs.dto.*;
import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Register the user
        User user = authService.register(req);

        // Automatically log the user in right after registration
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Force session creation → browser will receive JSESSIONID cookie
        request.getSession(true);

        // CRITICAL FIX: Save SecurityContext to session (required for manual auth)
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        // Log the login event
        authService.saveLoginHistory(user, request);

        // Prepare response with user data
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

        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = (User) auth.getPrincipal();

        // Force session creation
        request.getSession(true);

        // CRITICAL FIX: Save SecurityContext to session (required for manual auth)
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        authService.saveLoginHistory(user, request);

        // Prepare response body
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

        // Optional hint for frontend
        response.setHeader("X-Redirect-To", "/");

        return ResponseEntity.ok(res);
    }

    // ────────────────────────────────────────────────
    //           PASSWORD RESET ENDPOINTS
    // ────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            authService.initiatePasswordReset(req.getEmail());
            return ResponseEntity.ok("If an account exists with this email, reset instructions have been sent. Check your inbox and spam folder.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            authService.resetPassword(req.getToken(), req.getNewPassword());
            return ResponseEntity.ok("Password has been successfully reset. You can now log in with your new password.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest req) {
        try {
            authService.changePassword(user, req.getCurrentPassword(), req.getNewPassword());
            return ResponseEntity.ok("Your password has been changed successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
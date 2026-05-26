package com.webs.furniturewebs.config;

import com.webs.furniturewebs.entity.User;
import com.webs.furniturewebs.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    public CustomOAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String sub = oAuth2User.getAttribute("sub");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        User user = authService.findOrCreateGoogleUser(email, sub, firstName, lastName);
        authService.saveLoginHistory(user, request);

        UsernamePasswordAuthenticationToken updatedAuth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(updatedAuth);

        // Redirect first-time Google users to select language
        if (user.getAuthProvider() == com.webs.furniturewebs.entity.AuthProvider.GOOGLE
                && Boolean.FALSE.equals(user.getLanguageSelected())) {
            response.sendRedirect("/select-language.html");
            return;
        }

        super.onAuthenticationSuccess(request, response, updatedAuth);
    }
}
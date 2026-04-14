package com.devsecwatch.backend.security;

import com.devsecwatch.backend.model.User;
import com.devsecwatch.backend.model.enums.UserRole;
import com.devsecwatch.backend.repository.UserRepository;
import com.devsecwatch.backend.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        String email = oAuth2User.getAttribute("email");
        String login = oAuth2User.getAttribute("login");
        String name = oAuth2User.getAttribute("name");
        
        if (email == null) {
            email = login + "@github.com";
        }
        if (name == null) {
            name = login;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .username(name.replaceAll("\\s+", "").toLowerCase())
                    .passwordHash("") // No password for OAuth users
                    .role(UserRole.USER)
                    .build();
            user = userRepository.save(user);
        }

        String token = jwtService.generateAccessToken(user.getUsername());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

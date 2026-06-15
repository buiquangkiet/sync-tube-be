package com.example.synctube.security;

import com.example.synctube.entity.AuthProvider;
import com.example.synctube.entity.User;
import com.example.synctube.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final FrontendUrlResolver frontendUrlResolver;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        AuthProvider provider = switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> AuthProvider.LOCAL;
        };

        String providerId = String.valueOf(attributes.get("sub") != null
                ? attributes.get("sub")
                : attributes.get("id"));

        User user = userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException("OAuth user not persisted"));

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        String redirectUrl = UriComponentsBuilder.fromUriString(
                        frontendUrlResolver.resolve(request) + "/auth/callback")
                .queryParam("token", token)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

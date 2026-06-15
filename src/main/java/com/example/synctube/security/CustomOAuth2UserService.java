package com.example.synctube.security;

import com.example.synctube.entity.AuthProvider;
import com.example.synctube.entity.User;
import com.example.synctube.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = mapProvider(registrationId);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String providerId = String.valueOf(attributes.get("sub") != null
                ? attributes.get("sub")
                : attributes.get("id"));
        String email = resolveEmail(attributes, providerId);
        String displayName = resolveDisplayName(attributes, email);
        String avatarUrl = resolveAvatarUrl(attributes, registrationId);

        User user = userRepository.findByAuthProviderAndProviderId(provider, providerId)
                .orElseGet(() -> createOAuthUser(provider, providerId, email, displayName, avatarUrl));

        if (displayName != null && !displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
        }
        if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);

        return oauth2User;
    }

    private User createOAuthUser(
            AuthProvider provider,
            String providerId,
            String email,
            String displayName,
            String avatarUrl) {
        String username = provider.name().toLowerCase() + "_" + providerId;
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        if (userRepository.existsByEmail(email)) {
            email = providerId + "+" + email;
        }

        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .displayName(displayName)
                .authProvider(provider)
                .providerId(providerId)
                .avatarUrl(avatarUrl)
                .build());
    }

    private AuthProvider mapProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> AuthProvider.GOOGLE;
            case "facebook" -> AuthProvider.FACEBOOK;
            default -> AuthProvider.LOCAL;
        };
    }

    private String resolveEmail(Map<String, Object> attributes, String providerId) {
        Object email = attributes.get("email");
        if (email != null && !email.toString().isBlank()) {
            return email.toString();
        }
        return providerId + "@oauth.synctube.local";
    }

    private String resolveDisplayName(Map<String, Object> attributes, String email) {
        Object name = attributes.get("name");
        if (name != null && !name.toString().isBlank()) {
            return name.toString();
        }
        return email.split("@")[0];
    }

    @SuppressWarnings("unchecked")
    private String resolveAvatarUrl(Map<String, Object> attributes, String registrationId) {
        if ("google".equalsIgnoreCase(registrationId)) {
            Object picture = attributes.get("picture");
            return picture != null ? picture.toString() : null;
        }
        if ("facebook".equalsIgnoreCase(registrationId)) {
            Object picture = attributes.get("picture");
            if (picture instanceof Map<?, ?> pictureMap) {
                Object data = pictureMap.get("data");
                if (data instanceof Map<?, ?> dataMap) {
                    Object url = dataMap.get("url");
                    return url != null ? url.toString() : null;
                }
            }
        }
        return null;
    }
}

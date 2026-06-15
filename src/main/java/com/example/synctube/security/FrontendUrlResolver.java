package com.example.synctube.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrontendUrlResolver {

    @Value("${app.frontend.url:http://localhost:5174}")
    private String configuredFrontendUrl;

    public String resolve(HttpServletRequest request) {
        String forwardedHost = firstHeaderValue(request, "X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            String scheme = firstHeaderValue(request, "X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) {
                scheme = "https";
            }
            return scheme + "://" + forwardedHost;
        }

        String host = request.getHeader("Host");
        if (host != null && !host.isBlank() && !host.startsWith("localhost")
                && !host.startsWith("127.0.0.1")) {
            String scheme = request.isSecure() ? "https" : "http";
            return scheme + "://" + host;
        }

        return trimTrailingSlash(configuredFrontendUrl);
    }

    private static String firstHeaderValue(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

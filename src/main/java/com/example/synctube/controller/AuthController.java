package com.example.synctube.controller;

import com.example.synctube.dto.response.UserResponse;
import com.example.synctube.security.SecurityUtils;
import com.example.synctube.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public UserResponse me() {
        UserPrincipal principal = SecurityUtils.requireAuthenticated();
        return UserResponse.from(principal.getUser());
    }

    @PostMapping("/logout")
    public void logout() {
        SecurityUtils.requireAuthenticated();
        // Stateless JWT — client discards token
    }
}

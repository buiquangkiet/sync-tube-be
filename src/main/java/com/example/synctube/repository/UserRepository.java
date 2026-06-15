package com.example.synctube.repository;

import com.example.synctube.entity.AuthProvider;
import com.example.synctube.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}

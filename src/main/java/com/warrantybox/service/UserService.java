package com.warrantybox.service;

import com.warrantybox.model.User;
import com.warrantybox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles user registration and doubles as the Spring Security
 * UserDetailsService (users log in with their email as the username).
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Thrown for any registration validation failure. */
    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {
            super(message);
        }
    }

    public User register(String name, String email, String password, String confirmPassword) {
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            throw new RegistrationException("All fields are required.");
        }
        if (!password.equals(confirmPassword)) {
            throw new RegistrationException("Passwords do not match.");
        }
        if (password.length() < 6) {
            throw new RegistrationException("Password must be at least 6 characters long.");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RegistrationException("An account with this email already exists.");
        }

        User user = User.builder()
                .name(name.trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No account found for this email."));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}

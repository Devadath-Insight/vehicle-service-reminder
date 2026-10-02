package com.vehicleservice.service;

import com.vehicleservice.dto.PasswordChangeRequest;
import com.vehicleservice.dto.ProfileUpdateRequest;
import com.vehicleservice.dto.RegistrationRequest;
import com.vehicleservice.exception.ResourceNotFoundException;
import com.vehicleservice.model.User;
import com.vehicleservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Locale;

/**
 * User accounts: registration, profile, password change and login lookup.
 *
 * POLYMORPHISM: this class implements Spring Security's UserDetailsService interface.
 * Spring Security only knows the interface and calls loadUserByUsername(...) on it;
 * at runtime our implementation is the one that runs.
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return email != null && userRepository.existsByEmail(normalizeEmail(email));
    }

    public User register(RegistrationRequest request) {
        if (emailExists(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = new User(
                request.getFullName().trim(),
                normalizeEmail(request.getEmail()),
                passwordEncoder.encode(request.getPassword()),   // BCrypt hash - never store plain text
                request.getPhone().trim());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
    }

    /** Returns the logged-in user as an entity (Spring Security gives us only the email). */
    @Transactional(readOnly = true)
    public User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("You are not logged in.");
        }
        return getByEmail(principal.getName());
    }

    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone().trim());
        return user;   // saved automatically at the end of the transaction (dirty checking)
    }

    /**
     * @throws IllegalArgumentException with a user-friendly message if a rule fails
     */
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

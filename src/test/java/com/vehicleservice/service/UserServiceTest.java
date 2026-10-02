package com.vehicleservice.service;

import com.vehicleservice.TestData;
import com.vehicleservice.dto.PasswordChangeRequest;
import com.vehicleservice.dto.ProfileUpdateRequest;
import com.vehicleservice.model.User;
import com.vehicleservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Registration, BCrypt password hashing, login lookup, profile and password change. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional   // every test is rolled back, so tests never affect each other
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerSavesUserWithNormalisedEmail() {
        User user = userService.register(TestData.registration("  New.User@Example.COM "));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("new.user@example.com");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("new.user@example.com")).isPresent();
    }

    @Test
    void passwordIsStoredAsBcryptHashNotPlainText() {
        User user = userService.register(TestData.registration("hash@example.com"));

        assertThat(user.getPassword()).isNotEqualTo(TestData.PASSWORD);
        assertThat(user.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(TestData.PASSWORD, user.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailIsRejected() {
        userService.register(TestData.registration("dup@example.com"));

        assertThatThrownBy(() -> userService.register(TestData.registration("DUP@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void mismatchedPasswordsAreRejected() {
        var request = TestData.registration("mismatch@example.com");
        request.setConfirmPassword("somethingElse1");

        assertThatThrownBy(() -> userService.register(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loadUserByUsernameFindsRegisteredUser() {
        userService.register(TestData.registration("login@example.com"));

        UserDetails details = userService.loadUserByUsername("LOGIN@example.com");

        assertThat(details.getUsername()).isEqualTo("login@example.com");
        assertThat(passwordEncoder.matches(TestData.PASSWORD, details.getPassword())).isTrue();
    }

    @Test
    void loadUserByUsernameFailsForUnknownEmail() {
        assertThatThrownBy(() -> userService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void updateProfileChangesNameAndPhone() {
        User user = userService.register(TestData.registration("profile@example.com"));
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Updated Name");
        request.setPhone("+919999988888");

        userService.updateProfile(user.getId(), request);

        User reloaded = userService.getByEmail("profile@example.com");
        assertThat(reloaded.getFullName()).isEqualTo("Updated Name");
        assertThat(reloaded.getPhone()).isEqualTo("+919999988888");
    }

    @Test
    void changePasswordRequiresCorrectCurrentPassword() {
        User user = userService.register(TestData.registration("pw1@example.com"));
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("brandNew123");
        request.setConfirmPassword("brandNew123");

        assertThatThrownBy(() -> userService.changePassword(user.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void changePasswordStoresNewBcryptHash() {
        User user = userService.register(TestData.registration("pw2@example.com"));
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(TestData.PASSWORD);
        request.setNewPassword("brandNew123");
        request.setConfirmPassword("brandNew123");

        userService.changePassword(user.getId(), request);

        User reloaded = userService.getByEmail("pw2@example.com");
        assertThat(passwordEncoder.matches("brandNew123", reloaded.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(TestData.PASSWORD, reloaded.getPassword())).isFalse();
    }
}

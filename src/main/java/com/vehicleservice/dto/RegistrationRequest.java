package com.vehicleservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Data submitted by the registration form. "Passwords match" and "email unique" are checked in AuthController. */
public class RegistrationRequest {

    @NotBlank(message = "Enter your full name")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
    private String fullName;

    @NotBlank(message = "Enter your email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @NotBlank(message = "Enter your phone number")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter 10 to 15 digits, optionally starting with +")
    private String phone;

    @NotBlank(message = "Choose a password")
    @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
    private String password;

    @NotBlank(message = "Confirm your password")
    private String confirmPassword;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

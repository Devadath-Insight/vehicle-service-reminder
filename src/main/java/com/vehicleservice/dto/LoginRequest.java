package com.vehicleservice.dto;

/**
 * Backing object for the login form. The actual authentication is performed by
 * Spring Security, which reads the "email", "password" and "rememberMe" request parameters.
 */
public class LoginRequest {

    private String email;
    private String password;
    private boolean rememberMe;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}

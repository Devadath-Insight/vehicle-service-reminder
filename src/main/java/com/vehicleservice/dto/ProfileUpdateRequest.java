package com.vehicleservice.dto;

import com.vehicleservice.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Editable profile fields. Email is the login name, so it is not editable here. */
public class ProfileUpdateRequest {

    @NotBlank(message = "Enter your full name")
    @Size(min = 2, max = 100, message = "Name must be 2 to 100 characters")
    private String fullName;

    @NotBlank(message = "Enter your phone number")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Enter 10 to 15 digits, optionally starting with +")
    private String phone;

    public static ProfileUpdateRequest from(User user) {
        ProfileUpdateRequest form = new ProfileUpdateRequest();
        form.setFullName(user.getFullName());
        form.setPhone(user.getPhone());
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

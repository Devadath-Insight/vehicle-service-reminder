package com.vehicleservice.controller;

import com.vehicleservice.dto.PasswordChangeRequest;
import com.vehicleservice.dto.ProfileUpdateRequest;
import com.vehicleservice.model.User;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Profile page: view details, update name/phone, change password. */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final VehicleService vehicleService;

    public ProfileController(UserService userService, VehicleService vehicleService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String profile(Principal principal, Model model) {
        User user = userService.getCurrentUser(principal);
        populate(model, user, ProfileUpdateRequest.from(user), new PasswordChangeRequest());
        return "profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateRequest form,
                                BindingResult bindingResult, Principal principal, Model model,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        if (bindingResult.hasErrors()) {
            populate(model, user, form, new PasswordChangeRequest());
            return "profile";
        }
        userService.updateProfile(user.getId(), form);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeRequest form,
                                 BindingResult bindingResult, Principal principal, Model model,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        if (!bindingResult.hasFieldErrors("newPassword") && !bindingResult.hasFieldErrors("confirmPassword")
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "New passwords do not match");
        }
        if (!bindingResult.hasErrors()) {
            try {
                userService.changePassword(user.getId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Password changed.");
                return "redirect:/profile";
            } catch (IllegalArgumentException ex) {
                String field = ex.getMessage().startsWith("Current") ? "currentPassword" : "newPassword";
                bindingResult.rejectValue(field, "invalid", ex.getMessage());
            }
        }
        populate(model, user, ProfileUpdateRequest.from(user), form);
        model.addAttribute("openPasswordSection", true);
        return "profile";
    }

    private void populate(Model model, User user, ProfileUpdateRequest profileForm, PasswordChangeRequest passwordForm) {
        model.addAttribute("user", user);
        model.addAttribute("vehicleCount", vehicleService.countVehicles(user.getId()));
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("passwordForm", passwordForm);
    }
}

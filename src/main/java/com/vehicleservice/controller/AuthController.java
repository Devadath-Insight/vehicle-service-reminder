package com.vehicleservice.controller;

import com.vehicleservice.dto.LoginRequest;
import com.vehicleservice.dto.RegistrationRequest;
import com.vehicleservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Login and registration pages. The login POST itself is handled by Spring Security
 * (configured in SecurityConfig), so there is no @PostMapping("/login") here.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Principal principal, Model model) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasFieldErrors("password") && !bindingResult.hasFieldErrors("confirmPassword")
                && !request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasFieldErrors("email") && userService.emailExists(request.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "An account with this email already exists. Log in instead.");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        userService.register(request);
        redirectAttributes.addFlashAttribute("successMessage", "Account created. Log in with your email and password.");
        return "redirect:/login";
    }
}

package com.example.demo.controller;

import com.example.demo.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    // ── Login ─────────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ── Register GET ─────────────────────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    // ── Register POST ─────────────────────────────────────────────────────────
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result,
                           RedirectAttributes flash,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(form.getUsername(), form.getPassword(), form.getEmail());
            flash.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ── Form DTO ──────────────────────────────────────────────────────────────
    @Data
    public static class RegisterForm {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        private String email;
    }
}

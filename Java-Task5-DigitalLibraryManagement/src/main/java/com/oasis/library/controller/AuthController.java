package com.oasis.library.controller;

import com.oasis.library.model.User;
import com.oasis.library.repository.BookRepository;
import com.oasis.library.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserRepository users;
    private final BookRepository books;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, BookRepository books, PasswordEncoder encoder) {
        this.users = users;
        this.books = books;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bookCount", books.count());
        model.addAttribute("copyCount", books.totalCopies());
        model.addAttribute("categoryCount", books.findCategories().size());
        model.addAttribute("memberCount", users.countByRole(User.Role.USER));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
                           RedirectAttributes ra) {
        if (!result.hasFieldErrors("email") && users.existsByEmailIgnoreCase(form.getEmail().trim()))
            result.rejectValue("email", "dup", "An account with this email already exists");
        if (!result.hasFieldErrors("confirmPassword") && !form.getPassword().equals(form.getConfirmPassword()))
            result.rejectValue("confirmPassword", "match", "Passwords do not match");
        if (result.hasErrors()) return "auth/register";

        users.save(new User(form.getFullName().trim(), form.getEmail().trim().toLowerCase(),
                form.getPhone().trim(), encoder.encode(form.getPassword()), User.Role.USER));
        ra.addFlashAttribute("success", "Account created! Please sign in.");
        return "redirect:/login";
    }

    @GetMapping("/error/403")
    public String forbidden() {
        return "error/403";
    }

    public static class RegisterForm {
        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 60, message = "Name must be 3-60 characters")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        private String email;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
        private String phone;

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*\\d)(?=.*[A-Za-z]).{8,}$", message = "Min 8 characters with at least one letter and one number")
        private String password;

        @NotBlank(message = "Please confirm your password")
        private String confirmPassword;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
}

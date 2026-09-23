package com.warrantybox.controller;

import com.warrantybox.model.User;
import com.warrantybox.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String landing(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("name")) {
            model.addAttribute("name", "");
            model.addAttribute("email", "");
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                            @RequestParam String email,
                            @RequestParam String password,
                            @RequestParam("confirmPassword") String confirmPassword,
                            Model model) {
        try {
            User user = userService.register(name, email, password, confirmPassword);
            model.addAttribute("successMessage",
                    "Account created for " + user.getName() + "! You can now log in.");
            return "auth/login";
        } catch (UserService.RegistrationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            return "auth/register";
        }
    }
}

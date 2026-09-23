package com.warrantybox.controller;

import com.warrantybox.model.Product;
import com.warrantybox.model.User;
import com.warrantybox.service.ProductService;
import com.warrantybox.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                             @RequestParam(required = false) String query,
                             @RequestParam(required = false) String status,
                             Model model) {
        User currentUser = userService.findByEmail(authentication.getName());
        List<Product> allProducts = productService.findAllForUser(currentUser.getId());
        List<Product> visibleProducts = productService.searchAndFilter(currentUser.getId(), query, status);

        long activeCount = allProducts.stream()
                .filter(p -> p.getWarrantyStatus() == Product.WarrantyStatus.ACTIVE).count();
        long expiringCount = allProducts.stream()
                .filter(p -> p.getWarrantyStatus() == Product.WarrantyStatus.EXPIRING_SOON).count();
        long expiredCount = allProducts.stream()
                .filter(p -> p.getWarrantyStatus() == Product.WarrantyStatus.EXPIRED).count();

        model.addAttribute("user", currentUser);
        model.addAttribute("products", visibleProducts);
        model.addAttribute("totalCount", allProducts.size());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("expiringCount", expiringCount);
        model.addAttribute("expiredCount", expiredCount);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("status", status == null ? "ALL" : status);

        return "dashboard";
    }
}

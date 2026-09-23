package com.warrantybox.controller;

import com.warrantybox.model.Product;
import com.warrantybox.model.User;
import com.warrantybox.service.ProductService;
import com.warrantybox.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    private User currentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName());
    }

    // ---------- List / Search ----------

    @GetMapping
    public String list(Authentication authentication,
                        @RequestParam(required = false) String query,
                        @RequestParam(required = false) String status,
                        Model model) {
        User user = currentUser(authentication);
        List<Product> products = productService.searchAndFilter(user.getId(), query, status);
        model.addAttribute("products", products);
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("status", status == null ? "ALL" : status);
        return "products/products";
    }

    // ---------- Add ----------

    @GetMapping("/add")
    public String addForm(Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        model.addAttribute("isEdit", false);
        return "products/add-product";
    }

    @PostMapping("/add")
    public String add(Authentication authentication,
                       @ModelAttribute Product product,
                       @RequestParam(value = "invoiceFile", required = false) MultipartFile invoiceFile,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        User user = currentUser(authentication);
        try {
            productService.addProduct(user.getId(), product, invoiceFile);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully.");
            return "redirect:/dashboard";
        } catch (ProductService.ProductException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("product", product);
            model.addAttribute("isEdit", false);
            return "products/add-product";
        }
    }

    // ---------- Edit ----------

    @GetMapping("/edit/{id}")
    public String editForm(Authentication authentication, @PathVariable String id, Model model) {
        User user = currentUser(authentication);
        Product product = productService.getOwnedProduct(id, user.getId());
        model.addAttribute("product", product);
        model.addAttribute("isEdit", true);
        return "products/add-product";
    }

    @PostMapping("/edit/{id}")
    public String edit(Authentication authentication,
                        @PathVariable String id,
                        @ModelAttribute Product product,
                        @RequestParam(value = "invoiceFile", required = false) MultipartFile invoiceFile,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        User user = currentUser(authentication);
        try {
            productService.updateProduct(id, user.getId(), product, invoiceFile);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
            return "redirect:/products/" + id;
        } catch (ProductService.ProductException e) {
            model.addAttribute("errorMessage", e.getMessage());
            product.setId(id);
            model.addAttribute("product", product);
            model.addAttribute("isEdit", true);
            return "products/add-product";
        }
    }

    // ---------- Delete ----------

    @PostMapping("/delete/{id}")
    public String delete(Authentication authentication, @PathVariable String id, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        try {
            productService.deleteProduct(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted.");
        } catch (ProductService.ProductException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // ---------- Details ----------

    @GetMapping("/{id}")
    public String details(Authentication authentication, @PathVariable String id, Model model) {
        User user = currentUser(authentication);
        Product product = productService.getOwnedProduct(id, user.getId());
        model.addAttribute("product", product);
        return "products/product-details";
    }

    // ---------- Invoice download/view ----------

    @GetMapping("/invoice/{id}")
    public ResponseEntity<Resource> invoice(Authentication authentication, @PathVariable String id) {
        User user = currentUser(authentication);
        Product product = productService.getOwnedProduct(id, user.getId());

        if (!product.isInvoiceUploaded()) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(product.getInvoiceFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType;
        try {
            contentType = Files.probeContentType(file.toPath());
        } catch (Exception e) {
            contentType = null;
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + product.getInvoiceFileName() + "\"")
                .body(resource);
    }

    // ---------- Friendly error handling for this controller ----------

    @ExceptionHandler(ProductService.ProductException.class)
    public String handleProductException(ProductService.ProductException e,
                                          RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/dashboard";
    }
}

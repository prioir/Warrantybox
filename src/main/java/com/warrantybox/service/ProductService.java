package com.warrantybox.service;

import com.warrantybox.model.Product;
import com.warrantybox.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * All business logic for products: CRUD, ownership checks, warranty date
 * math, invoice file handling, and simple search/filter.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    public static class ProductException extends RuntimeException {
        public ProductException(String message) {
            super(message);
        }
    }

    // ---------- Create ----------

    public Product addProduct(String userId, Product form, MultipartFile invoice) {
        LocalDate warrantyEnd = calculateWarrantyEndDate(form.getPurchaseDate(), form.getWarrantyPeriod());

        Product product = Product.builder()
                .userId(userId)
                .name(form.getName())
                .brand(form.getBrand())
                .category(form.getCategory())
                .price(form.getPrice())
                .purchaseDate(form.getPurchaseDate())
                .warrantyPeriod(form.getWarrantyPeriod())
                .warrantyEndDate(warrantyEnd)
                .serialNumber(form.getSerialNumber())
                .storeName(form.getStoreName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        storeInvoiceIfPresent(invoice, product);

        return productRepository.save(product);
    }

    // ---------- Read ----------

    public List<Product> findAllForUser(String userId) {
        return productRepository.findByUserId(userId);
    }

    /** Fetch a product, verifying it belongs to the given user. */
    public Product getOwnedProduct(String id, String userId) {
        return productRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ProductException("Product not found or you do not have access to it."));
    }

    /** Simple in-memory search + status filter over the user's own products. */
    public List<Product> searchAndFilter(String userId, String query, String status) {
        List<Product> products = productRepository.findByUserId(userId);

        if (StringUtils.hasText(query)) {
            String q = query.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> containsIgnoreCase(p.getName(), q)
                            || containsIgnoreCase(p.getBrand(), q)
                            || containsIgnoreCase(p.getCategory(), q)
                            || containsIgnoreCase(p.getSerialNumber(), q))
                    .toList();
        }

        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            products = products.stream()
                    .filter(p -> p.getWarrantyStatus().name().equalsIgnoreCase(status))
                    .toList();
        }

        return products;
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    // ---------- Update ----------

    public Product updateProduct(String id, String userId, Product form, MultipartFile invoice) {
        Product existing = getOwnedProduct(id, userId);

        existing.setName(form.getName());
        existing.setBrand(form.getBrand());
        existing.setCategory(form.getCategory());
        existing.setPrice(form.getPrice());
        existing.setPurchaseDate(form.getPurchaseDate());
        existing.setWarrantyPeriod(form.getWarrantyPeriod());
        existing.setWarrantyEndDate(calculateWarrantyEndDate(form.getPurchaseDate(), form.getWarrantyPeriod()));
        existing.setSerialNumber(form.getSerialNumber());
        existing.setStoreName(form.getStoreName());
        existing.setUpdatedAt(LocalDateTime.now());

        // Replace the invoice only if a new file was actually uploaded.
        if (invoice != null && !invoice.isEmpty()) {
            deleteInvoiceFileQuietly(existing.getInvoiceFilePath());
            storeInvoiceIfPresent(invoice, existing);
        }

        return productRepository.save(existing);
    }

    // ---------- Delete ----------

    public void deleteProduct(String id, String userId) {
        Product existing = getOwnedProduct(id, userId);
        deleteInvoiceFileQuietly(existing.getInvoiceFilePath());
        productRepository.delete(existing);
    }

    // ---------- Warranty math ----------

    public LocalDate calculateWarrantyEndDate(LocalDate purchaseDate, Integer warrantyPeriodYears) {
        if (purchaseDate == null || warrantyPeriodYears == null) {
            return null;
        }
        return purchaseDate.plusYears(warrantyPeriodYears);
    }

    // ---------- Invoice file handling ----------

    private void storeInvoiceIfPresent(MultipartFile invoice, Product product) {
        if (invoice == null || invoice.isEmpty()) {
            return; // No invoice uploaded - app still works fine without one.
        }

        String originalName = StringUtils.cleanPath(
                invoice.getOriginalFilename() == null ? "" : invoice.getOriginalFilename());
        String extension = getExtension(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ProductException("Invoice must be a PDF, JPG, JPEG, or PNG file.");
        }
        if (invoice.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ProductException("Invoice file is too large. Maximum size is 5MB.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(invoice.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            product.setInvoiceFileName(originalName);
            product.setInvoiceFilePath(targetPath.toString());
        } catch (IOException e) {
            throw new ProductException("Failed to store invoice file. Please try again.");
        }
    }

    private void deleteInvoiceFileQuietly(String path) {
        if (!StringUtils.hasText(path)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException ignored) {
            // Non-fatal: the DB record still gets updated even if the old file can't be removed.
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}

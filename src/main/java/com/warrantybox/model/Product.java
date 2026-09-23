package com.warrantybox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A product owned by a user, with its warranty details.
 * Warranty status and days-remaining are calculated on the fly (in
 * {@link #getWarrantyStatus()} / {@link #getDaysRemaining()}) rather than
 * stored, so they are always accurate against "today".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    /** Owning user's id - every product belongs to exactly one user. */
    private String userId;

    private String name;
    private String brand;
    private String category;
    private Double price;

    private LocalDate purchaseDate;

    /** Warranty length in years, e.g. 2 = "2 Years". */
    private Integer warrantyPeriod;

    /** Automatically calculated from purchaseDate + warrantyPeriod. */
    private LocalDate warrantyEndDate;

    private String serialNumber;
    private String storeName;

    /** Original uploaded file name, shown to the user. */
    private String invoiceFileName;

    /** Path on disk where the invoice is stored. */
    private String invoiceFilePath;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum WarrantyStatus {
        ACTIVE, EXPIRING_SOON, EXPIRED
    }

    /**
     * ACTIVE: more than 30 days remaining.
     * EXPIRING_SOON: 1-30 days remaining.
     * EXPIRED: warranty end date has passed.
     */
    public WarrantyStatus getWarrantyStatus() {
        if (warrantyEndDate == null) {
            return WarrantyStatus.EXPIRED;
        }
        long days = getDaysRemaining();
        if (days < 0) {
            return WarrantyStatus.EXPIRED;
        } else if (days <= 30) {
            return WarrantyStatus.EXPIRING_SOON;
        } else {
            return WarrantyStatus.ACTIVE;
        }
    }

    /** Days left until warrantyEndDate (negative if already expired). */
    public long getDaysRemaining() {
        if (warrantyEndDate == null) {
            return -1;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), warrantyEndDate);
    }

    public boolean isInvoiceUploaded() {
        return invoiceFilePath != null && !invoiceFilePath.isBlank();
    }
}

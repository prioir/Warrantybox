package com.warrantybox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A registered WarrantyBox user. Passwords are always stored BCrypt-hashed,
 * never in plain text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    /** BCrypt-hashed password - never plain text. */
    private String password;

    /** Single simple role; no admin system needed for this app. */
    @Builder.Default
    private String role = "USER";

    private LocalDateTime createdAt;
}

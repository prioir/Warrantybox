package com.warrantybox.repository;

import com.warrantybox.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    /** All products belonging to a specific user. */
    List<Product> findByUserId(String userId);

    /**
     * Fetch a product only if it belongs to the given user - the core of our
     * ownership check, used everywhere a product is viewed/edited/deleted.
     */
    Optional<Product> findByIdAndUserId(String id, String userId);
}

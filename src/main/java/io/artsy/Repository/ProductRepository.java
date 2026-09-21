package io.artsy.Repository;

import io.artsy.Model.Product;
import io.artsy.Model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByProductTypeIgnoreCase(String productType);
    List<Product> findByVendor(Vendor vendor);
    List<Product> findByTitleContainingIgnoreCase(String title);
}
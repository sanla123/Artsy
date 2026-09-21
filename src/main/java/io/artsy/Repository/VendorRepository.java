package io.artsy.Repository;

import io.artsy.Model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Optional<Vendor> findByUser_Username(String username);
    Optional<Vendor> findByUser_Id(int userId);
}
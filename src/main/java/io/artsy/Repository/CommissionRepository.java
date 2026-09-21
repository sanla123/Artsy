package io.artsy.Repository;

import io.artsy.Model.Commission;
import io.artsy.Model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {
    List<Commission> findByCustomer_Username(String username);
    List<Commission> findByVendor(Vendor vendor);
}
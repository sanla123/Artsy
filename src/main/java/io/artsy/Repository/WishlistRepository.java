package io.artsy.Repository;

import io.artsy.Model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    List<Wishlist> findByUser_Username(String username);
    Optional<Wishlist> findByUser_UsernameAndProduct_Id(String username, int productId);
    boolean existsByUser_UsernameAndProduct_Id(String username, int productId);
}
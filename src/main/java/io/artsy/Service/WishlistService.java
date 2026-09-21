package io.artsy.Service;

import io.artsy.DTO.WishlistDTO;
import io.artsy.DTO.WishlistRequest;
import io.artsy.Model.Product;
import io.artsy.Model.UserTbl;
import io.artsy.Model.Wishlist;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    @Autowired
    private WishlistRepository wishlistRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private UserRepository userRepo;

    public List<WishlistDTO> getWishlistForUser(String username) {
        return wishlistRepo.findByUser_Username(username).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Optional<WishlistDTO> addToWishlist(String username, int productId) {
        Optional<UserTbl> userOpt = userRepo.findByUsername(username);
        Optional<Product> productOpt = productRepo.findById(productId);

        if (userOpt.isEmpty() || productOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<Wishlist> existing = wishlistRepo.findByUser_UsernameAndProduct_Id(username, productId);
        if (existing.isPresent()) {
            return Optional.of(toDTO(existing.get()));
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(userOpt.get());
        wishlist.setProduct(productOpt.get());

        return Optional.of(toDTO(wishlistRepo.save(wishlist)));
    }

    public boolean removeFromWishlistByProduct(String username, int productId) {
        Optional<Wishlist> existing = wishlistRepo.findByUser_UsernameAndProduct_Id(username, productId);
        if (existing.isPresent()) {
            wishlistRepo.delete(existing.get());
            return true;
        }
        return false;
    }

    private WishlistDTO toDTO(Wishlist w) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(w.getId());
        dto.setProductId(w.getProduct().getId());
        dto.setProductTitle(w.getProduct().getTitle());
        dto.setProductPrice(w.getProduct().getPrice());
        dto.setProductImage(w.getProduct().getImage());
        return dto;
    }
}
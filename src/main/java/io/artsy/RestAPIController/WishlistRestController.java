package io.artsy.RestAPIController;

import io.artsy.DTO.WishlistDTO;
import io.artsy.DTO.WishlistRequest;
import io.artsy.Service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistRestController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<?> getMyWishlist(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }
        List<WishlistDTO> items = wishlistService.getWishlistForUser(username);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> addToWishlist(@RequestBody WishlistRequest request, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }

        Optional<WishlistDTO> result = wishlistService.addToWishlist(username, request.getProductId());

        if (result.isPresent()) {
            return ResponseEntity.status(201).body(result.get());
        }
        return ResponseEntity.badRequest().body("That product couldn't be found.");
    }

    @DeleteMapping("/product/{productId}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable int productId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }

        if (wishlistService.removeFromWishlistByProduct(username, productId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
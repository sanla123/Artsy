package io.artsy.RestAPIController;

import io.artsy.DTO.CartSummaryDTO;
import io.artsy.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartRestController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }
        return ResponseEntity.ok(cartService.getCartSummary(username));
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Integer> body, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }
        Integer productId = body.get("productId");
        if (productId == null || !cartService.addToCart(username, productId)) {
            return ResponseEntity.badRequest().body("That product couldn't be added.");
        }
        return ResponseEntity.status(201).body(cartService.getCartSummary(username));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<?> updateQuantity(@PathVariable int itemId, @RequestBody Map<String, String> body, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }
        String action = body.get("action");
        return cartService.updateQuantity(username, itemId, action)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable int itemId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).body("You need to be logged in.");
        }
        return cartService.removeItem(username, itemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
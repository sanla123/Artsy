package io.artsy.Controller;

import io.artsy.Model.Product;
import io.artsy.Model.UserTbl;
import io.artsy.Model.Wishlist;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.WishlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class WishlistController {

    @Autowired
    private WishlistRepository wishlistRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/wishlist")
    public String wishlistGet(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        UserTbl currentUser = userRepo.findByUsername(username).orElse(null);
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return "redirect:/admin";
        }
        if (currentUser != null && "vendor".equals(currentUser.getRole())) {
            return "redirect:/vendor";
        }

        List<Wishlist> items = wishlistRepo.findByUser_Username(username);
        m.addAttribute("items", items);

        return "wishlistPage.html";
    }

    @PostMapping("/wishlist/toggle")
    public String toggleWishlist(@RequestParam("productId") int productId,
                                 HttpSession session,
                                 HttpServletRequest request) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Wishlist> existing = wishlistRepo.findByUser_UsernameAndProduct_Id(username, productId);

        if (existing.isPresent()) {
            wishlistRepo.delete(existing.get());
        } else {
            UserTbl user = userRepo.findByUsername(username).orElse(null);
            Optional<Product> productOpt = productRepo.findById(productId);

            if (user != null && productOpt.isPresent()) {
                Wishlist wishlist = new Wishlist();
                wishlist.setUser(user);
                wishlist.setProduct(productOpt.get());
                wishlistRepo.save(wishlist);
            }
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/shop");
    }

    @PostMapping("/wishlist/remove/{id}")
    public String removeFromWishlist(@PathVariable int id, HttpSession session) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        wishlistRepo.deleteById(id);

        return "redirect:/wishlist";
    }

}
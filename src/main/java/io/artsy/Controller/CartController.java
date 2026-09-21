package io.artsy.Controller;

import io.artsy.Model.OrderItem;
import io.artsy.Model.UserTbl;
import io.artsy.Repository.UserRepository;
import io.artsy.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/cart")
    public String cartGet(Model m, HttpSession session,
                          @RequestParam(value = "ordered", required = false) String ordered) {
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

        List<OrderItem> items = cartService.getCartItems(username);
        double subtotal = items.stream().mapToDouble(io.artsy.Model.OrderItem::getSubtotal).sum();

        m.addAttribute("items", items);
        m.addAttribute("subtotal", subtotal);
        m.addAttribute("justOrdered", ordered != null);

        return "cartPage.html";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("productId") int productId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        cartService.addToCart(username, productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/buy-now")
    public String buyNow(@RequestParam("productId") int productId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Integer> orderId = cartService.buyNow(username, productId);
        if (orderId.isEmpty()) {
            return "redirect:/shop";
        }

        return "redirect:/checkout?orderId=" + orderId.get();
    }

    @PostMapping("/cart/update/{itemId}")
    public String updateQuantity(@PathVariable int itemId,
                                 @RequestParam("action") String action,
                                 HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        cartService.updateQuantity(username, itemId, action);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{itemId}")
    public String removeItem(@PathVariable int itemId, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        cartService.removeItem(username, itemId);
        return "redirect:/cart";
    }

}
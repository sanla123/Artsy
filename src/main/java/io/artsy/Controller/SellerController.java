package io.artsy.Controller;

import io.artsy.Model.Product;
import io.artsy.Model.UserTbl;
import io.artsy.Model.Vendor;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.VendorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
public class SellerController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private VendorRepository vendorRepo;

    @GetMapping("/seller/{id}")
    public String sellerGet(@PathVariable int id, Model m, HttpSession session) {

        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl currentUser = userRepo.findByUsername(username).orElse(null);
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return "redirect:/admin";
        }

        Optional<Vendor> vendorOpt = vendorRepo.findById(id);
        if (vendorOpt.isEmpty()) {
            return "redirect:/shop";
        }

        Vendor vendor = vendorOpt.get();
        List<Product> products = productRepo.findByVendor(vendor);

        String shopName = vendor.getShopName();
        if (shopName == null || shopName.isBlank()) {
            shopName = vendor.getUser() != null ? vendor.getUser().getUsername() : "Independent creator";
        }

        m.addAttribute("shopName", shopName);
        m.addAttribute("profileImage", vendor.getUser() != null ? vendor.getUser().getProfileImage() : null);
        m.addAttribute("products", products);

        return "sellerPage";
    }

}
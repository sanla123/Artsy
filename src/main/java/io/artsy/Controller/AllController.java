package io.artsy.Controller;

import io.artsy.Model.UserTbl;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.stream.Collectors;

@Controller
public class AllController {
    @Autowired
    private UserRepository uRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private WishlistRepository wishlistRepo;


    @GetMapping("/")
    public String firstPage()
    {
        return "firstPage.html";
    }


    @GetMapping("/home")
    public String homeGet(Model m, HttpSession session)
    {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl currentUser = uRepo.findByUsername(username).orElse(null);
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return "redirect:/admin";
        }
        if (currentUser != null && "vendor".equals(currentUser.getRole())) {
            return "redirect:/vendor";
        }

        var wishlistedIds = wishlistRepo.findByUser_Username(username).stream()
                .map(w -> w.getProduct().getId())
                .collect(Collectors.toSet());

        m.addAttribute("username", session.getAttribute("username"));
        m.addAttribute("products", productRepo.findAll());
        m.addAttribute("wishlistedIds", wishlistedIds);

        return "home.html";
    }

    @GetMapping("/about")
    public String aboutGet()
    {
        return "aboutPage.html";
    }

}
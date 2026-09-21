package io.artsy.Controller;

import io.artsy.Model.Product;
import io.artsy.Model.UserTbl;
import io.artsy.Repository.OrderItemRepository;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.WishlistRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WishlistRepository wishlistRepo;

    @Autowired
    private OrderItemRepository orderItemRepo;

    @GetMapping("/shop")
    public String shopGet(@RequestParam(value = "category", required = false) String category,
                          @RequestParam(value = "search", required = false) String search,
                          @RequestParam(value = "priceRange", required = false) String priceRange,
                          Model m, HttpSession session) {

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

        List<Product> products;
        if (search != null && !search.isEmpty()) {
            products = productRepo.findByTitleContainingIgnoreCase(search);
        } else if (category != null && !category.isEmpty()) {
            products = productRepo.findByProductTypeIgnoreCase(category);
        } else {
            products = productRepo.findAll();
        }

        if (priceRange != null && !priceRange.isEmpty()) {
            products = products.stream().filter(p -> {
                if ("under500".equals(priceRange)) return p.getPrice() < 500;
                if ("500to1000".equals(priceRange)) return p.getPrice() >= 500 && p.getPrice() <= 1000;
                if ("1000plus".equals(priceRange)) return p.getPrice() > 1000;
                return true;
            }).collect(Collectors.toList());
        }

        Set<Integer> wishlistedIds = wishlistRepo.findByUser_Username(username)
                .stream()
                .map(w -> w.getProduct().getId())
                .collect(Collectors.toSet());

        m.addAttribute("products", products);
        m.addAttribute("selectedCategory", category);
        m.addAttribute("searchTerm", search);
        m.addAttribute("selectedPriceRange", priceRange);
        m.addAttribute("wishlistedIds", wishlistedIds);

        return "shopPage.html";
    }

    @GetMapping("/product/{id}")
    public String productDetailsGet(@PathVariable int id, Model m, HttpSession session) {

        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl currentUser = userRepo.findByUsername(username).orElse(null);
        if (currentUser != null && "admin".equals(currentUser.getRole())) {
            return "redirect:/admin";
        }

        Optional<Product> productOpt = productRepo.findById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/shop";
        }

        Product product = productOpt.get();
        m.addAttribute("product", product);
        m.addAttribute("isWishlisted", wishlistRepo.existsByUser_UsernameAndProduct_Id(username, id));

        List<Product> related = productRepo.findByProductTypeIgnoreCase(product.getProductType())
                .stream()
                .filter(p -> p.getId() != product.getId())
                .limit(4)
                .collect(Collectors.toList());

        m.addAttribute("relatedProducts", related);

        return "productDetailsPage.html";
    }

    @GetMapping("/product/download/{id}")
    public ResponseEntity<byte[]> downloadDigitalProduct(@PathVariable int id, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Product> productOpt = productRepo.findById(id);
        if (productOpt.isEmpty() || !productOpt.get().isDigital()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();

        boolean purchased = orderItemRepo.existsByProduct_IdAndOrder_User_UsernameAndOrder_Status(id, username, "placed");
        if (!purchased) {
            return ResponseEntity.status(403).build();
        }

        if (product.getDigitalFileData() == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = product.getDigitalFileName() != null ? product.getDigitalFileName() : (product.getTitle() + ".bin");

        MediaType mediaType;
        try {
            mediaType = product.getDigitalFileType() != null
                    ? MediaType.parseMediaType(product.getDigitalFileType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(product.getDigitalFileData());
    }

}
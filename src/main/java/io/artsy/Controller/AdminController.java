package io.artsy.Controller;

import io.artsy.Model.UserTbl;
import io.artsy.Model.Vendor;
import io.artsy.Repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private CommissionRepository commissionRepo;
    @Autowired private VendorRepository vendorRepo;

    @GetMapping("/admin")
    public String adminGet(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty() || !"admin".equals(userOpt.get().getRole())) {
            return "redirect:/home";
        }

        loadStats(m);

        return "adminPage.html";
    }

    @GetMapping("/admin/products")
    public String adminProducts(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty() || !"admin".equals(userOpt.get().getRole())) {
            return "redirect:/home";
        }

        m.addAttribute("allProducts", productRepo.findAll());
        return "adminProductsPage.html";
    }

    @PostMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable int id, Model m, HttpSession session) {
        String adminUsername = (String) session.getAttribute("username");
        if (adminUsername == null) {
            return "redirect:/login";
        }

        var adminOpt = userRepo.findByUsername(adminUsername);
        if (adminOpt.isEmpty() || !"admin".equals(adminOpt.get().getRole())) {
            return "redirect:/home";
        }

        Optional<UserTbl> targetOpt = userRepo.findById(id);
        if (targetOpt.isEmpty()) {
            loadStats(m);
            m.addAttribute("error", "That user no longer exists.");
            return "adminPage.html";
        }

        UserTbl target = targetOpt.get();

        if (target.getId() == adminOpt.get().getId()) {
            loadStats(m);
            m.addAttribute("error", "You can't delete your own admin account from here.");
            return "adminPage.html";
        }

        try {
            Optional<Vendor> vendorOpt = vendorRepo.findByUser_Id(target.getId());
            vendorOpt.ifPresent(vendorRepo::delete);

            userRepo.delete(target);

            loadStats(m);
            m.addAttribute("message", "Deleted user \"" + target.getUsername() + "\".");
            return "adminPage.html";

        } catch (DataIntegrityViolationException e) {
            loadStats(m);
            m.addAttribute("error", "Can't delete \"" + target.getUsername() + "\" — they still have products, orders, or other linked data.");
            return "adminPage.html";
        }
    }

    private void loadStats(Model m) {
        m.addAttribute("totalUsers", userRepo.count());
        m.addAttribute("totalVendors", vendorRepo.count());
        m.addAttribute("totalProducts", productRepo.count());
        m.addAttribute("totalOrders", orderRepo.count());
        m.addAttribute("totalCommissions", commissionRepo.count());
        m.addAttribute("allUsers", userRepo.findAll());
    }

}
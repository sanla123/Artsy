package io.artsy.Controller;

import io.artsy.Model.UserTbl;
import io.artsy.Model.Vendor;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.VendorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class SettingsController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private VendorRepository vendorRepo;

    @GetMapping("/settings")
    public String settingsGet(@RequestParam(value = "updated", required = false) String updated,
                              Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        m.addAttribute("user", user);

        if ("vendor".equals(user.getRole())) {
            Optional<Vendor> vendorOpt = vendorRepo.findByUser_Username(username);
            vendorOpt.ifPresent(v -> m.addAttribute("vendor", v));
        }

        if (updated != null) {
            m.addAttribute("message", "Your settings were updated.");
        }

        return "settingsPage.html";
    }

    @PostMapping("/settings/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        String hashedCurrent = DigestUtils.md5DigestAsHex(currentPassword.getBytes());

        if (!hashedCurrent.equals(user.getPassword())) {
            reloadSettingsModel(m, user);
            m.addAttribute("error", "Your current password is incorrect.");
            return "settingsPage.html";
        }

        if (newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
            reloadSettingsModel(m, user);
            m.addAttribute("error", "New passwords don't match, or are empty.");
            return "settingsPage.html";
        }

        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        userRepo.save(user);

        return "redirect:/settings?updated=true";
    }

    @PostMapping("/settings/vendor")
    public String updateVendorInfo(@RequestParam("shopName") String shopName,
                                   @RequestParam("contactInfo") String contactInfo,
                                   HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Vendor> vendorOpt = vendorRepo.findByUser_Username(username);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            vendor.setShopName(shopName);
            vendor.setContactInfo(contactInfo);
            vendorRepo.save(vendor);
        }

        return "redirect:/settings?updated=true";
    }

    @PostMapping("/settings/delete")
    public String deleteAccount(@RequestParam("confirmPassword") String confirmPassword,
                                Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        String hashedConfirm = DigestUtils.md5DigestAsHex(confirmPassword.getBytes());
        if (!hashedConfirm.equals(user.getPassword())) {
            reloadSettingsModel(m, user);
            m.addAttribute("error", "Incorrect password. Account was not deleted.");
            return "settingsPage.html";
        }

        try {
            Optional<Vendor> vendorOpt = vendorRepo.findByUser_Username(username);
            vendorOpt.ifPresent(vendorRepo::delete);

            userRepo.delete(user);

            session.invalidate();
            m.addAttribute("message", "Your account has been deleted. We're sorry to see you go.");
            return "loginPage.html";

        } catch (DataIntegrityViolationException e) {
            reloadSettingsModel(m, user);
            m.addAttribute("error", "Your account still has linked data (products, orders, or wishlist items). Please remove these first, then try deleting your account again.");
            return "settingsPage.html";
        }
    }

    private void reloadSettingsModel(Model m, UserTbl user) {
        m.addAttribute("user", user);
        if ("vendor".equals(user.getRole())) {
            vendorRepo.findByUser_Username(user.getUsername()).ifPresent(v -> m.addAttribute("vendor", v));
        }
    }

}
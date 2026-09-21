package io.artsy.Controller;

import io.artsy.Repository.WishlistRepository;
import io.artsy.Model.UserTbl;
import io.artsy.Repository.OrderRepository;
import io.artsy.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Controller
public class AccountController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private WishlistRepository wishlistRepo;

    @GetMapping("/profile")
    public String profileGet(@RequestParam(value = "updated", required = false) String updated,
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
        m.addAttribute("placedOrderCount",
                orderRepo.findByUser_UsernameAndStatusOrderByOrderDateDesc(username, "placed").size());
        m.addAttribute("wishlistCount", wishlistRepo.findByUser_Username(username).size());

        if (updated != null) {
            m.addAttribute("message", "Your profile was updated.");
        }

        return "profilePage.html";
    }

    @GetMapping("/profile/edit")
    public String editProfileGet(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        UserTbl user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        m.addAttribute("user", user);
        return "profileEditPage.html";
    }

    @PostMapping("/profile/edit")
    public String editProfilePost(@RequestParam("username") String newUsername,
                                  @RequestParam("email") String newEmail,
                                  @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
                                  Model m, HttpSession session) {
        String currentUsername = (String) session.getAttribute("username");
        if (currentUsername == null) {
            return "redirect:/login";
        }

        UserTbl user = userRepo.findByUsername(currentUsername).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        newUsername = newUsername == null ? "" : newUsername.trim();
        newEmail = newEmail == null ? "" : newEmail.trim();

        if (newUsername.isEmpty() || newEmail.isEmpty() || !newEmail.contains("@")) {
            m.addAttribute("user", user);
            m.addAttribute("error", "Please enter a valid username and email address.");
            return "profileEditPage.html";
        }

        boolean usernameChanged = !newUsername.equals(user.getUsername());
        boolean emailChanged = !newEmail.equals(user.getEmail());

        if (usernameChanged && userRepo.existsByUsername(newUsername)) {
            m.addAttribute("user", user);
            m.addAttribute("error", "That username is already taken.");
            return "profileEditPage.html";
        }

        if (emailChanged && userRepo.existsByEmail(newEmail)) {
            m.addAttribute("user", user);
            m.addAttribute("error", "That email is already in use.");
            return "profileEditPage.html";
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            String encoded = toSmallProfilePicture(profileImage);
            if (encoded == null) {
                m.addAttribute("user", user);
                m.addAttribute("error", "Please choose a valid image (JPG, PNG or GIF).");
                return "profileEditPage.html";
            }
            user.setProfileImage(encoded);
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);
        userRepo.save(user);

        // Keep the session in sync if the username changed, so you don't get logged out
        session.setAttribute("username", newUsername);

        return "redirect:/profile?updated=true";
    }

    // Crops the picture to a centered square, shrinks it to 300x300 and returns it as a small data URL.
    // Returns null if the file is not a readable image.
    private String toSmallProfilePicture(MultipartFile file) {
        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return null;
            }

            int side = Math.min(original.getWidth(), original.getHeight());
            int x = (original.getWidth() - side) / 2;
            int y = (original.getHeight() - side) / 2;
            BufferedImage square = original.getSubimage(x, y, side, side);

            int size = 300;
            BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, size, size);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(square, 0, 0, size, size, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

}
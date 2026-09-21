package io.artsy.Controller;

import io.artsy.Model.UserTbl;
import io.artsy.Model.Vendor;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.VendorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor //autowired
public class SignupLoginController {

    private final JavaMailSender jms;

    private  final UserRepository uRepo ;

    private final VendorRepository vendorRepo;


    @GetMapping("/signup")
    public String signup()
    {
        return "signupPage";
    }

    @GetMapping("/login")
    public String login()
    {
        return "loginPage";
    }


    @PostMapping("/signup")
    public String signupPost(HttpServletRequest request)
    {
        //request.getParameter("username") name?->Intellij ->form name
        String username=   request.getParameter("username");
        String password= request.getParameter("password");
        String email= request.getParameter("email");
        String roleChoice = request.getParameter("role");

        String hashPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        //md5 algorithm, this is basic algorithm, anyone can hack this
        //we will learn bcrypt technique very soon

        String token = UUID.randomUUID().toString();

        UserTbl user = new UserTbl();
        user.setUsername(username);
        user.setPassword(hashPassword);
        user.setEmail(email);
        user.setVerified(false);
        user.setVerificationToken(token);

        if ("vendor".equals(roleChoice)) {
            user.setRole("vendor");
        } else {
            user.setRole("customer");
        }

        uRepo.save(user);

        if ("vendor".equals(roleChoice)) {
            Vendor vendor = new Vendor();
            vendor.setUser(user);
            vendorRepo.save(vendor);
        }

        String verifyLink = "http://localhost:9090/verify-email?token=" + token;

//Mail Sender
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Confirm your Artsy account");

        message.setText("Welcome to Artsy, " + username + "!\n\n"
                + "Please confirm your email by clicking this link:\n" + verifyLink
                + "\n\nYou won't be able to log in until you verify your email.");

        jms.send(message);

        return "loginPage";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model m)
    {
        Optional<UserTbl> userOpt = uRepo.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            m.addAttribute("error", "This verification link is invalid or has already been used.");
            return "loginPage";
        }

        UserTbl user = userOpt.get();
        user.setVerified(true);
        user.setVerificationToken(null);
        uRepo.save(user);

        m.addAttribute("message", "Your email is verified! You can now log in.");
        return "loginPage";
    }

    @PostMapping("/login")
    public String loginPost(HttpServletRequest request, Model m)
    {
        String username= request.getParameter("username");
        String password = request.getParameter("password");

        String hashPassword = DigestUtils.md5DigestAsHex(password.getBytes());

        if( uRepo.existsByUsernameAndPassword(username,hashPassword))
        {
            Optional<UserTbl> userOpt = uRepo.findByUsername(username);

            if (userOpt.isPresent() && !userOpt.get().isVerified()) {
                m.addAttribute("error", "Please verify your email before logging in. Check your inbox for the confirmation link.");
                return "loginPage";
            }

            HttpSession session= request.getSession();
            session.setAttribute("username",username);

            if (userOpt.isPresent() && "admin".equals(userOpt.get().getRole())) {
                return "redirect:/admin";
            }

            if (userOpt.isPresent() && "vendor".equals(userOpt.get().getRole())) {
                return "redirect:/vendor";
            }

            return "redirect:/home";
        }

        m.addAttribute("error", "Username or password is incorrect");
        return "loginPage";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpSession session, Model m)
    {

        session.invalidate();

        m.addAttribute("message","You have logged out!");
        return "loginPage";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordGet()
    {
        return "forgotPasswordPage";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordPost(HttpServletRequest request, Model m)
    {
        String username = request.getParameter("username");
        String email = request.getParameter("email");

        Optional<UserTbl> userOpt = uRepo.findByUsernameAndEmail(username, email);

        if (userOpt.isEmpty()) {
            m.addAttribute("error", "That username and email don't match any Artsy account.");
            return "forgotPasswordPage";
        }

        UserTbl user = userOpt.get();

        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        String hashedNewPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());

        user.setPassword(hashedNewPassword);
        uRepo.save(user);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your Artsy password has been reset");
        message.setText("Hi " + user.getUsername() + ",\n\nYour new temporary password is: " + newPassword
                + "\n\nPlease log in with this password. You can change it later from your Settings page.");

        jms.send(message);

        m.addAttribute("message", "A new password has been sent to your email.");
        return "loginPage";
    }

}
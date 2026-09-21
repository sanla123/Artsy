package io.artsy.Controller;

import io.artsy.Model.Commission;
import io.artsy.Model.Message;
import io.artsy.Model.UserTbl;
import io.artsy.Model.Vendor;
import io.artsy.Repository.CommissionRepository;
import io.artsy.Repository.MessageRepository;
import io.artsy.Repository.UserRepository;
import io.artsy.Repository.VendorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class MessagesController {

    @Autowired
    private VendorRepository vendorRepo;

    @Autowired
    private CommissionRepository commissionRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private MessageRepository messageRepo;

    @GetMapping("/messages")
    public String messagesGet(@RequestParam(value = "edit", required = false) Integer edit,
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

        m.addAttribute("vendors", vendorRepo.findAll());
        m.addAttribute("myRequests", commissionRepo.findByCustomer_Username(username));
        m.addAttribute("editId", edit);

        return "messagesPage.html";
    }

    @GetMapping("/messages/{id}")
    public String requestThreadGet(@PathVariable int id, Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || !commissionOpt.get().getCustomer().getUsername().equals(username)) {
            return "redirect:/messages";
        }

        Commission commission = commissionOpt.get();
        m.addAttribute("commission", commission);
        m.addAttribute("messages", messageRepo.findByCommissionOrderBySentAtAsc(commission));
        m.addAttribute("currentUsername", username);

        return "messageThreadPage.html";
    }

    @PostMapping("/messages/{id}/reply")
    public String sendCustomerMessage(@PathVariable int id,
                                      @RequestParam(value = "content", required = false) String content,
                                      @RequestParam(value = "image", required = false) MultipartFile image,
                                      HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || !commissionOpt.get().getCustomer().getUsername().equals(username)) {
            return "redirect:/messages";
        }

        Commission commission = commissionOpt.get();
        UserTbl customer = userRepo.findByUsername(username).orElse(null);
        if (customer == null) {
            return "redirect:/messages";
        }

        boolean hasText = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasText && !hasImage) {
            return "redirect:/messages/" + id;
        }

        Message message = new Message();
        message.setSender(customer);
        message.setReceiver(commission.getVendor().getUser());
        message.setCommission(commission);
        message.setContent(hasText ? content.trim() : null);

        if (hasImage) {
            String type = image.getContentType();
            if (type == null || !type.startsWith("image/")) {
                return "redirect:/messages/" + id + "?error=Please+choose+an+image+file.";
            }
            if (image.getSize() > 2 * 1024 * 1024) {
                return "redirect:/messages/" + id + "?error=That+photo+is+too+big.+Please+choose+one+under+2MB.";
            }
            try {
                message.setImage("data:" + type + ";base64," + Base64.getEncoder().encodeToString(image.getBytes()));
            } catch (java.io.IOException e) {
                return "redirect:/messages/" + id + "?error=There+was+a+problem+reading+your+photo.";
            }
        }

        message.setSentAt(LocalDateTime.now());
        messageRepo.save(message);

        return "redirect:/messages/" + id;
    }

    @PostMapping("/messages/request")
    public String requestCommission(HttpServletRequest request, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        int vendorId;
        try {
            vendorId = Integer.parseInt(request.getParameter("vendorId"));
        } catch (NumberFormatException | NullPointerException e) {
            return "redirect:/messages?error=Please+choose+a+creator.";
        }

        String description = request.getParameter("description");
        if (description == null || description.trim().isEmpty()) {
            return "redirect:/messages?error=Please+describe+what+you'd+like+made.";
        }

        String budgetStr = request.getParameter("budget");
        double budget;
        try {
            budget = (budgetStr == null || budgetStr.isEmpty()) ? 0 : Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            return "redirect:/messages?error=Please+enter+a+valid+budget.";
        }
        if (budget < 0) {
            return "redirect:/messages?error=Budget+can't+be+negative.";
        }

        Vendor vendor = vendorRepo.findById(vendorId).orElse(null);
        UserTbl customer = userRepo.findByUsername(username).orElse(null);

        if (vendor == null || customer == null) {
            return "redirect:/messages?error=That+creator+couldn't+be+found.";
        }

        Commission commission = new Commission();
        commission.setCustomer(customer);
        commission.setVendor(vendor);
        commission.setDescription(description);
        commission.setBudget(budget);
        commission.setStatus("pending");
        commission.setCreatedAt(LocalDateTime.now());

        commissionRepo.save(commission);

        return "redirect:/messages";
    }

    @PostMapping("/messages/edit/{id}")
    public String editCommission(@PathVariable int id, HttpServletRequest request, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || !commissionOpt.get().getCustomer().getUsername().equals(username)) {
            return "redirect:/messages";
        }

        Commission commission = commissionOpt.get();

        if (!"pending".equals(commission.getStatus())) {
            return "redirect:/messages?error=This+request+has+already+been+" + commission.getStatus() + ",+so+it+can't+be+edited.";
        }

        int vendorId;

        try {
            vendorId = Integer.parseInt(request.getParameter("vendorId"));
        } catch (NumberFormatException | NullPointerException e) {
            return "redirect:/messages?error=Please+choose+a+creator.";
        }

        String description = request.getParameter("description");
        if (description == null || description.trim().isEmpty()) {
            return "redirect:/messages?error=Please+describe+what+you'd+like+made.";
        }

        String budgetStr = request.getParameter("budget");
        double budget;
        try {
            budget = (budgetStr == null || budgetStr.isEmpty()) ? 0 : Double.parseDouble(budgetStr);
        } catch (NumberFormatException e) {
            return "redirect:/messages?error=Please+enter+a+valid+budget.";
        }
        if (budget < 0) {
            return "redirect:/messages?error=Budget+can't+be+negative.";
        }

        Vendor vendor = vendorRepo.findById(vendorId).orElse(null);
        if (vendor == null) {
            return "redirect:/messages?error=That+creator+couldn't+be+found.";
        }

        commission.setVendor(vendor);
        commission.setDescription(description);
        commission.setBudget(budget);

        commissionRepo.save(commission);

        return "redirect:/messages";
    }

    @PostMapping("/messages/delete/{id}")
    public String deleteCommission(@PathVariable int id, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || !commissionOpt.get().getCustomer().getUsername().equals(username)) {
            return "redirect:/messages";
        }

        Commission commission = commissionOpt.get();

        if (!"pending".equals(commission.getStatus())) {
            return "redirect:/messages?error=This+request+has+already+been+" + commission.getStatus() + ",+so+it+can't+be+cancelled.";
        }

        messageRepo.deleteAll(messageRepo.findByCommissionOrderBySentAtAsc(commission));
        commissionRepo.delete(commission);

        return "redirect:/messages";
    }

}
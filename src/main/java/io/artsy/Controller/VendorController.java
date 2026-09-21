package io.artsy.Controller;

import io.artsy.Model.Commission;
import io.artsy.Model.Message;
import io.artsy.Model.OrderItem;
import io.artsy.Model.Product;
import io.artsy.Model.Vendor;
import io.artsy.Repository.CommissionRepository;
import io.artsy.Repository.MessageRepository;
import io.artsy.Repository.OrderItemRepository;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.VendorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class VendorController {

    @Autowired
    private VendorRepository vendorRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private OrderItemRepository orderItemRepo;

    @Autowired
    private CommissionRepository commissionRepo;

    @Autowired
    private MessageRepository messageRepo;

    private Vendor requireVendor(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return null;
        return vendorRepo.findByUser_Username(username).orElse(null);
    }

    @GetMapping("/vendor")
    public String vendorGet(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Vendor> vendorOpt = vendorRepo.findByUser_Username(username);
        if (vendorOpt.isEmpty()) {
            m.addAttribute("isVendor", false);
            return "vendorPage.html";
        }

        Vendor vendor = vendorOpt.get();
        m.addAttribute("isVendor", true);
        m.addAttribute("vendor", vendor);

        List<OrderItem> placedItems = orderItemRepo.findByProduct_VendorAndOrder_Status(vendor, "placed");
        double totalRevenue = 0;
        int pendingCount = 0;
        for (OrderItem item : placedItems) {
            boolean cancelled = "cancelled".equals(item.getStatus());
            boolean digital = item.getProduct().isDigital();
            if (!cancelled) {
                if (digital) {
                    totalRevenue += item.getSubtotal();
                } else {
                    pendingCount++;
                }
            }
        }

        m.addAttribute("totalRevenue", totalRevenue);
        m.addAttribute("pendingCount", pendingCount);
        m.addAttribute("productCount", productRepo.findByVendor(vendor).size());
        m.addAttribute("commissionCount", commissionRepo.findByVendor(vendor).size());

        return "vendorPage.html";
    }

    @GetMapping("/vendor/revenue")
    public String revenueGet(Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        List<OrderItem> digitalItems = orderItemRepo.findByProduct_VendorAndOrder_Status(vendor, "placed")
                .stream()
                .filter(item -> item.getProduct().isDigital())
                .collect(Collectors.toList());

        double totalRevenue = 0;
        for (OrderItem item : digitalItems) {
            if (!"cancelled".equals(item.getStatus())) {
                totalRevenue += item.getSubtotal();
            }
        }

        m.addAttribute("vendor", vendor);
        m.addAttribute("totalRevenue", totalRevenue);
        m.addAttribute("salesItems", digitalItems);

        return "vendorRevenuePage.html";
    }

    @GetMapping("/vendor/pending-orders")
    public String pendingOrdersGet(Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        List<OrderItem> placedItems =
                orderItemRepo.findByProduct_VendorAndOrder_StatusAndStatusNot(
                                vendor, "placed", "cancelled")
                        .stream()
                        .filter(item -> !item.getProduct().isDigital())
                        .collect(Collectors.toList());

        m.addAttribute("vendor", vendor);
        m.addAttribute("orderItems", placedItems);

        return "vendorPendingOrderPage.html";
    }

    @PostMapping("/vendor/orders/cancel/{itemId}")
    public String cancelOrderItem(@PathVariable int itemId, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<OrderItem> itemOpt = orderItemRepo.findById(itemId);
        if (itemOpt.isPresent() && itemOpt.get().getProduct().getVendor().getId() == vendor.getId()) {
            OrderItem item = itemOpt.get();
            item.setStatus("cancelled");
            orderItemRepo.save(item);
        }

        return "redirect:/vendor/pending-orders";
    }

    @GetMapping("/vendor/products")
    public String productsGet(Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        m.addAttribute("vendor", vendor);
        m.addAttribute("myProducts", productRepo.findByVendor(vendor));

        return "vendorProductsPage.html";
    }

    @PostMapping("/vendor/products/add")
    public String addProduct(HttpServletRequest request,
                             @RequestParam("image") MultipartFile image,
                             @RequestParam(value = "digitalFile", required = false) MultipartFile digitalFile,
                             HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        String title = request.getParameter("title");
        String description = request.getParameter("description");
        String priceStr = request.getParameter("price");
        String productType = request.getParameter("productType");
        boolean digital = "digital".equalsIgnoreCase(productType);

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException | NullPointerException e) {
            return "redirect:/vendor?error=Please+enter+a+valid+price.";
        }
        if (price < 0) {
            return "redirect:/vendor?error=Price+can't+be+negative.";
        }

        Integer stock = 0;
        if (!digital) {
            String stockStr = request.getParameter("stock");
            try {
                stock = stockStr == null || stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                return "redirect:/vendor?error=Please+enter+a+valid+stock+quantity.";
            }
            if (stock < 0) {
                return "redirect:/vendor?error=Stock+can't+be+negative.";
            }
        }

        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setProductType(productType);
        product.setVendor(vendor);

        try {
            if (digital) {
                product.setStock(null);
                if (digitalFile != null && !digitalFile.isEmpty()) {
                    product.setDigitalFileData(digitalFile.getBytes());
                    product.setDigitalFileName(digitalFile.getOriginalFilename());
                    product.setDigitalFileType(digitalFile.getContentType());
                }
            } else {
                product.setStock(stock);
            }

            if (!image.isEmpty()) {
                String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
                product.setImage("data:" + image.getContentType() + ";base64," + base64Image);
            }
        } catch (java.io.IOException e) {
            return "redirect:/vendor?error=There+was+a+problem+reading+your+file.+Please+try+again.";
        }

        productRepo.save(product);

        return "redirect:/vendor";
    }

    @GetMapping("/vendor/products/edit/{id}")
    public String editProductGet(@PathVariable int id, Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Product> productOpt = productRepo.findById(id);
        if (productOpt.isEmpty() || productOpt.get().getVendor().getId() != vendor.getId()) {
            return "redirect:/vendor/products";
        }

        m.addAttribute("product", productOpt.get());
        return "editProductPage.html";
    }

    @PostMapping("/vendor/products/edit/{id}")
    public String editProductPost(@PathVariable int id, HttpServletRequest request,
                                  @RequestParam("image") MultipartFile image,
                                  @RequestParam(value = "digitalFile", required = false) MultipartFile digitalFile,
                                  HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Product> productOpt = productRepo.findById(id);
        if (productOpt.isEmpty() || productOpt.get().getVendor().getId() != vendor.getId()) {
            return "redirect:/vendor/products";
        }

        Product product = productOpt.get();

        double price;
        try {
            price = Double.parseDouble(request.getParameter("price"));
        } catch (NumberFormatException | NullPointerException e) {
            return "redirect:/vendor/products/edit/" + id + "?error=Please+enter+a+valid+price.";
        }
        if (price < 0) {
            return "redirect:/vendor/products/edit/" + id + "?error=Price+can't+be+negative.";
        }

        Integer stock = null;
        if (!product.isDigital()) {
            String stockStr = request.getParameter("stock");
            try {
                stock = stockStr == null || stockStr.isEmpty() ? 0 : Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                return "redirect:/vendor/products/edit/" + id + "?error=Please+enter+a+valid+stock+quantity.";
            }
            if (stock < 0) {
                return "redirect:/vendor/products/edit/" + id + "?error=Stock+can't+be+negative.";
            }
        }

        product.setTitle(request.getParameter("title"));
        product.setDescription(request.getParameter("description"));
        product.setPrice(price);

        try {
            if (product.isDigital()) {
                product.setStock(null);
                if (digitalFile != null && !digitalFile.isEmpty()) {
                    product.setDigitalFileData(digitalFile.getBytes());
                    product.setDigitalFileName(digitalFile.getOriginalFilename());
                    product.setDigitalFileType(digitalFile.getContentType());
                }
            } else {
                product.setStock(stock);
            }

            if (!image.isEmpty()) {
                String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
                product.setImage("data:" + image.getContentType() + ";base64," + base64Image);
            }
        } catch (java.io.IOException e) {
            return "redirect:/vendor/products/edit/" + id + "?error=There+was+a+problem+reading+your+file.+Please+try+again.";
        }

        productRepo.save(product);

        return "redirect:/vendor/products";
    }

    @PostMapping("/vendor/products/delete/{id}")
    public String deleteProduct(@PathVariable int id, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Product> productOpt = productRepo.findById(id);
        if (productOpt.isPresent() && productOpt.get().getVendor().getId() == vendor.getId()) {
            productRepo.deleteById(id);
        }

        return "redirect:/vendor/products";
    }

    @GetMapping("/vendor/custom-requests")
    public String customRequestsGet(Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        m.addAttribute("vendor", vendor);
        m.addAttribute("commissionRequests", commissionRepo.findByVendor(vendor));

        return "vendorCustomRequestPage.html";
    }

    @GetMapping("/vendor/custom-requests/{id}")
    public String requestThreadGet(@PathVariable int id, Model m, HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || commissionOpt.get().getVendor().getId() != vendor.getId()) {
            return "redirect:/vendor/custom-requests";
        }

        Commission commission = commissionOpt.get();
        m.addAttribute("vendor", vendor);
        m.addAttribute("commission", commission);
        m.addAttribute("messages", messageRepo.findByCommissionOrderBySentAtAsc(commission));

        return "vendorRequestThreadPage.html";
    }

    @PostMapping("/vendor/custom-requests/{id}/message")
    public String sendVendorMessage(@PathVariable int id,
                                    @RequestParam(value = "content", required = false) String content,
                                    @RequestParam(value = "image", required = false) MultipartFile image,
                                    HttpSession session) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isEmpty() || commissionOpt.get().getVendor().getId() != vendor.getId()) {
            return "redirect:/vendor/custom-requests";
        }

        Commission commission = commissionOpt.get();

        boolean hasText = content != null && !content.trim().isEmpty();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasText && !hasImage) {
            return "redirect:/vendor/custom-requests/" + id;
        }

        Message message = new Message();
        message.setSender(vendor.getUser());
        message.setReceiver(commission.getCustomer());
        message.setCommission(commission);
        message.setContent(hasText ? content.trim() : null);

        if (hasImage) {
            String type = image.getContentType();
            if (type == null || !type.startsWith("image/")) {
                return "redirect:/vendor/custom-requests/" + id + "?error=Please+choose+an+image+file.";
            }
            if (image.getSize() > 2 * 1024 * 1024) {
                return "redirect:/vendor/custom-requests/" + id + "?error=That+photo+is+too+big.+Please+choose+one+under+2MB.";
            }
            try {
                message.setImage("data:" + type + ";base64," + Base64.getEncoder().encodeToString(image.getBytes()));
            } catch (java.io.IOException e) {
                return "redirect:/vendor/custom-requests/" + id + "?error=There+was+a+problem+reading+your+photo.";
            }
        }

        message.setSentAt(LocalDateTime.now());
        messageRepo.save(message);

        return "redirect:/vendor/custom-requests/" + id;
    }

    @PostMapping("/vendor/commissions/{id}/approve")
    public String approveCommission(@PathVariable int id, HttpSession session) {
        return updateCommissionStatus(id, session, "approved");
    }

    @PostMapping("/vendor/commissions/{id}/reject")
    public String rejectCommission(@PathVariable int id, HttpSession session) {
        return updateCommissionStatus(id, session, "rejected");
    }

    private String updateCommissionStatus(int id, HttpSession session, String newStatus) {
        Vendor vendor = requireVendor(session);
        if (vendor == null) return "redirect:/login";

        Optional<Commission> commissionOpt = commissionRepo.findById(id);
        if (commissionOpt.isPresent() && commissionOpt.get().getVendor().getId() == vendor.getId()) {
            Commission commission = commissionOpt.get();
            commission.setStatus(newStatus);
            commissionRepo.save(commission);
        }

        return "redirect:/vendor/custom-requests";
    }

}
package io.artsy.Controller;

import io.artsy.Model.Order;
import io.artsy.Model.OrderItem;
import io.artsy.Model.Product;
import io.artsy.Repository.OrderItemRepository;
import io.artsy.Repository.OrderRepository;
import io.artsy.Repository.ProductRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CheckoutController {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private OrderItemRepository orderItemRepo;

    @Autowired
    private ProductRepository productRepo;

    @GetMapping("/checkout")
    public String checkoutGet(@RequestParam(value = "orderId", required = false) Integer orderId,
                              Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Order order;
        if (orderId != null) {
            Optional<Order> orderOpt = orderRepo.findById(orderId);
            if (orderOpt.isEmpty() || !orderOpt.get().getUser().getUsername().equals(username)
                    || !("cart".equals(orderOpt.get().getStatus()) || "buynow".equals(orderOpt.get().getStatus()))) {
                return "redirect:/cart";
            }
            order = orderOpt.get();
        } else {
            Optional<Order> cartOpt = orderRepo.findByUser_UsernameAndStatus(username, "cart");
            if (cartOpt.isEmpty()) {
                return "redirect:/cart";
            }
            order = cartOpt.get();
        }

        List<OrderItem> items = orderItemRepo.findByOrder(order);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }

        double subtotal = 0;
        for (OrderItem item : items) {
            subtotal += item.getSubtotal();
        }

        boolean containsDigital = items.stream().anyMatch(i -> i.getProduct().isDigital());

        m.addAttribute("items", items);
        m.addAttribute("subtotal", subtotal);
        m.addAttribute("containsDigital", containsDigital);
        m.addAttribute("orderId", order.getId());

        return "checkoutPage.html";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@RequestParam("orderId") int orderId, HttpServletRequest request, Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getUsername().equals(username)
                || !("cart".equals(orderOpt.get().getStatus()) || "buynow".equals(orderOpt.get().getStatus()))) {
            return "redirect:/cart";
        }

        Order order = orderOpt.get();
        List<OrderItem> items = orderItemRepo.findByOrder(order);

        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (!product.isDigital()) {
                int available = product.getStock() == null ? 0 : product.getStock();
                if (available < item.getQuantity()) {
                    double subtotal = 0;
                    for (OrderItem i : items) subtotal += i.getSubtotal();
                    m.addAttribute("items", items);
                    m.addAttribute("subtotal", subtotal);
                    m.addAttribute("error", "\"" + product.getTitle() + "\" only has " + available + " left in stock.");
                    return "checkoutPage.html";
                }
            }
        }

        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (!product.isDigital()) {
                product.setStock(product.getStock() - item.getQuantity());
                productRepo.save(product);
            }
        }

        boolean containsDigital = items.stream().anyMatch(i -> i.getProduct().isDigital());
        String paymentMethod = request.getParameter("paymentMethod");

        if (containsDigital && !"online".equals(paymentMethod)) {
            double subtotal = 0;
            for (OrderItem i : items) subtotal += i.getSubtotal();
            m.addAttribute("items", items);
            m.addAttribute("subtotal", subtotal);
            m.addAttribute("containsDigital", true);
            m.addAttribute("error", "Digital products require online payment — Cash on Delivery isn't available for this order.");
            return "checkoutPage.html";
        }

        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("online".equals(paymentMethod) ? "paid" : "unpaid");

        if (!containsDigital) {
            String fullName = request.getParameter("fullName");
            String address = request.getParameter("address");
            String phone = request.getParameter("phone");
            order.setShippingAddress(fullName + ", " + address + " — " + phone);
        } else {
            order.setShippingAddress("Digital order — no delivery required");
        }

        double orderTotal = 0;
        for (OrderItem i : items) orderTotal += i.getSubtotal();

        order.setTotalAmount(orderTotal);
        order.setStatus("placed");
        orderRepo.save(order);

        return "redirect:/checkout/confirmation?orderId=" + order.getId();
    }

    @GetMapping("/checkout/confirmation")
    public String confirmationGet(@RequestParam("orderId") int orderId,
                                  Model m, HttpSession session) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(orderId);
        orderOpt.ifPresent(order -> {
            m.addAttribute("order", order);
            m.addAttribute("orderItems", orderItemRepo.findByOrder(order));
        });

        return "checkoutConfirmationPage.html";
    }

    @GetMapping("/orders")
    public String myOrdersGet(Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        List<Order> orders = orderRepo.findByUser_UsernameAndStatusInOrderByOrderDateDesc(username, List.of("placed", "cancelled"));
        Map<Integer, List<OrderItem>> itemsByOrder = new LinkedHashMap<>();
        for (Order order : orders) {
            itemsByOrder.put(order.getId(), orderItemRepo.findByOrder(order));
        }

        m.addAttribute("orders", orders);
        m.addAttribute("itemsByOrder", itemsByOrder);

        return "myOrdersPage.html";
    }

    @GetMapping("/orders/{id}")
    public String orderDetailsGet(@PathVariable int id, Model m, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getUsername().equals(username)
                || !("placed".equals(orderOpt.get().getStatus()) || "cancelled".equals(orderOpt.get().getStatus()))) {
            return "redirect:/orders";
        }

        Order order = orderOpt.get();
        m.addAttribute("order", order);
        m.addAttribute("items", orderItemRepo.findByOrder(order));

        return "orderDetailsPage.html";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable int id, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isPresent() && orderOpt.get().getUser().getUsername().equals(username)
                && "placed".equals(orderOpt.get().getStatus())) {

            Order order = orderOpt.get();
            boolean hasDigital = orderItemRepo.findByOrder(order).stream()
                    .anyMatch(item -> item.getProduct().isDigital());

            if (!hasDigital) {
                order.setStatus("cancelled");
                orderRepo.save(order);
            }
        }

        return "redirect:/orders/" + id;
    }

}
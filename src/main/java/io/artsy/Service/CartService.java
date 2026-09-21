package io.artsy.Service;

import io.artsy.DTO.CartItemDTO;
import io.artsy.DTO.CartSummaryDTO;
import io.artsy.Model.Order;
import io.artsy.Model.OrderItem;
import io.artsy.Model.Product;
import io.artsy.Model.UserTbl;
import io.artsy.Repository.OrderItemRepository;
import io.artsy.Repository.OrderRepository;
import io.artsy.Repository.ProductRepository;
import io.artsy.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private OrderItemRepository orderItemRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private UserRepository userRepo;

    public CartSummaryDTO getCartSummary(String username) {
        return toSummary(getCartItems(username));
    }

    // Returns the raw entities — used by the cartPage.html view, which reads nested
    // fields like item.product.title that the DTO doesn't carry.
    public List<OrderItem> getCartItems(String username) {
        Optional<Order> cartOpt = orderRepo.findByUser_UsernameAndStatus(username, "cart");
        return cartOpt.isPresent() ? orderItemRepo.findByOrder(cartOpt.get()) : List.of();
    }

    public boolean addToCart(String username, int productId) {
        Optional<Product> productOpt = productRepo.findById(productId);
        Optional<UserTbl> userOpt = userRepo.findByUsername(username);
        if (productOpt.isEmpty() || userOpt.isEmpty()) {
            return false;
        }
        Product product = productOpt.get();

        Order cart = orderRepo.findByUser_UsernameAndStatus(username, "cart").orElseGet(() -> {
            Order newOrder = new Order();
            newOrder.setUser(userOpt.get());
            newOrder.setStatus("cart");
            newOrder.setOrderDate(LocalDateTime.now());
            newOrder.setTotalAmount(0);
            return orderRepo.save(newOrder);
        });

        Optional<OrderItem> existingItem = orderItemRepo.findByOrderAndProduct_Id(cart, productId);
        if (existingItem.isPresent()) {
            OrderItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + 1);
            item.setSubtotal(item.getQuantity() * product.getPrice());
            orderItemRepo.save(item);
        } else {
            OrderItem item = new OrderItem();
            item.setOrder(cart);
            item.setProduct(product);
            item.setQuantity(1);
            item.setSubtotal(product.getPrice());
            item.setStatus("pending");
            orderItemRepo.save(item);
        }

        recalculateOrderTotal(cart);
        return true;
    }

    public Optional<Integer> buyNow(String username, int productId) {
        Optional<Product> productOpt = productRepo.findById(productId);
        Optional<UserTbl> userOpt = userRepo.findByUsername(username);
        if (productOpt.isEmpty() || userOpt.isEmpty()) {
            return Optional.empty();
        }

        Order buyNowOrder = new Order();
        buyNowOrder.setUser(userOpt.get());
        buyNowOrder.setStatus("buynow");
        buyNowOrder.setOrderDate(LocalDateTime.now());
        orderRepo.save(buyNowOrder);

        OrderItem item = new OrderItem();
        item.setOrder(buyNowOrder);
        item.setProduct(productOpt.get());
        item.setQuantity(1);
        item.setSubtotal(productOpt.get().getPrice());
        item.setStatus("pending");
        orderItemRepo.save(item);

        return Optional.of(buyNowOrder.getId());
    }

    // Returns empty if the item doesn't exist OR doesn't belong to this user — same message either way,
    // so we never reveal whether an item exists in someone else's cart.
    public Optional<CartSummaryDTO> updateQuantity(String username, int itemId, String action) {
        Optional<OrderItem> itemOpt = findOwnedItem(username, itemId);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }

        OrderItem item = itemOpt.get();
        if ("increase".equals(action)) {
            item.setQuantity(item.getQuantity() + 1);
        } else if ("decrease".equals(action) && item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
        }
        item.setSubtotal(item.getQuantity() * item.getProduct().getPrice());
        orderItemRepo.save(item);
        recalculateOrderTotal(item.getOrder());

        return Optional.of(getCartSummary(username));
    }

    public Optional<CartSummaryDTO> removeItem(String username, int itemId) {
        Optional<OrderItem> itemOpt = findOwnedItem(username, itemId);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }

        Order order = itemOpt.get().getOrder();
        orderItemRepo.deleteById(itemId);
        recalculateOrderTotal(order);

        return Optional.of(getCartSummary(username));
    }

    private Optional<OrderItem> findOwnedItem(String username, int itemId) {
        Optional<OrderItem> itemOpt = orderItemRepo.findById(itemId);
        if (itemOpt.isEmpty() || !itemOpt.get().getOrder().getUser().getUsername().equals(username)) {
            return Optional.empty();
        }
        return itemOpt;
    }

    private void recalculateOrderTotal(Order order) {
        List<OrderItem> items = orderItemRepo.findByOrder(order);
        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        order.setTotalAmount(total);
        orderRepo.save(order);
    }

    private CartSummaryDTO toSummary(List<OrderItem> items) {
        CartSummaryDTO summary = new CartSummaryDTO();
        double subtotal = 0;
        List<CartItemDTO> dtos = items.stream().map(item -> {
            CartItemDTO dto = new CartItemDTO();
            dto.setId(item.getId());
            dto.setProductId(item.getProduct().getId());
            dto.setProductTitle(item.getProduct().getTitle());
            dto.setProductImage(item.getProduct().getImage());
            dto.setPrice(item.getProduct().getPrice());
            dto.setQuantity(item.getQuantity());
            dto.setSubtotal(item.getSubtotal());
            return dto;
        }).collect(Collectors.toList());

        for (CartItemDTO dto : dtos) {
            subtotal += dto.getSubtotal();
        }

        summary.setItems(dtos);
        summary.setSubtotal(subtotal);
        return summary;
    }
}
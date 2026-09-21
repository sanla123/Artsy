package io.artsy.Repository;

import io.artsy.Model.Order;
import io.artsy.Model.OrderItem;
import io.artsy.Model.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findByOrder(Order order);

    Optional<OrderItem> findByOrderAndProduct_Id(Order order, int productId);

    List<OrderItem> findByProduct_VendorAndOrder_Status(Vendor vendor, String status);

    List<OrderItem> findByProduct_VendorAndOrder_StatusAndStatusNot(
            Vendor vendor, String orderStatus, String itemStatus);

    boolean existsByProduct_IdAndOrder_User_UsernameAndOrder_Status(
            int productId, String username, String status);
}
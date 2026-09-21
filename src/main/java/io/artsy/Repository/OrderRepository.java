package io.artsy.Repository;

import io.artsy.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByUser_UsernameAndStatus(String username, String status);
    List<Order> findByUser_UsernameAndStatusOrderByOrderDateDesc(String username, String status);
    List<Order> findByUser_UsernameAndStatusInOrderByOrderDateDesc(String username, List<String> statuses);
}
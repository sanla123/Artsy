package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserTbl user;

    private LocalDateTime orderDate;
    private double totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    private String shippingAddress;

}
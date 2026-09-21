package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Commission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private UserTbl customer;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    private String description;
    private double budget;
    private String status;
    private LocalDateTime createdAt;

}
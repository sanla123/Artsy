package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserTbl user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

}
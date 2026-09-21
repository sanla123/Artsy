package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserTbl user;

    private String shopName;
    private String contactInfo;

}
package io.artsy.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

//@Entity creates the table with provided name in database
//UserTbl -> user_tbl
@Entity
@Data
public class UserTbl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String email;
    private String role;
    private boolean verified;
    private String verificationToken;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String profileImage;
}
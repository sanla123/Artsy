package io.artsy.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;
    private String description;
    private double price;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;
    private Integer stock;
    private String productType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] digitalFileData;
    private String digitalFileName;
    private String digitalFileType;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    public boolean isDigital() {
        return "digital".equalsIgnoreCase(productType);
    }

}
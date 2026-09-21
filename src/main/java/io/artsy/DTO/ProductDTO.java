package io.artsy.DTO;

import lombok.Data;

@Data
public class ProductDTO {
    private int id;
    private String title;
    private String description;
    private double price;
    private String image;
    private Integer stock;
    private String productType;
    private String categoryName;
    private int vendorId;
    private String vendorShopName;
}
package io.artsy.DTO;

import lombok.Data;

@Data
public class ProductRequest {
    private String title;
    private String description;
    private double price;
    private Integer stock;
    private String productType;
    private String image;
    private int vendorId;
    private int categoryId;
}
package io.artsy.DTO;

import lombok.Data;

@Data
public class WishlistDTO {
    private int id;
    private int productId;
    private String productTitle;
    private double productPrice;
    private String productImage;
}
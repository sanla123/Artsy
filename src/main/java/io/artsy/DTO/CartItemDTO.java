package io.artsy.DTO;

import lombok.Data;

@Data
public class CartItemDTO {
    private int id;
    private int productId;
    private String productTitle;
    private String productImage;
    private double price;
    private int quantity;
    private double subtotal;
}
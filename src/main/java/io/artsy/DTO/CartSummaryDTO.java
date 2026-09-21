package io.artsy.DTO;

import lombok.Data;
import java.util.List;

@Data
public class CartSummaryDTO {
    private List<CartItemDTO> items;
    private double subtotal;
}
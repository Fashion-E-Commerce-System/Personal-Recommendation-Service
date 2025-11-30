package com.ecommerce.backend.document;

import lombok.*;

@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private Integer quantity;
}
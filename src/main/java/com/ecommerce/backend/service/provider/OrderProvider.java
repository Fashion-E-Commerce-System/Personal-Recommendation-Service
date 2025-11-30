package com.ecommerce.backend.service.provider;

import com.ecommerce.backend.document.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderProvider {
    List<Order> getOrders(LocalDateTime date);
}

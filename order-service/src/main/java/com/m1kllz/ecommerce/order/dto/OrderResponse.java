package com.m1kllz.ecommerce.order.dto;

import java.math.BigDecimal;

public record OrderResponse(Long id, String orderNumber, String skuCode,
        BigDecimal price, Integer quantity) {
}

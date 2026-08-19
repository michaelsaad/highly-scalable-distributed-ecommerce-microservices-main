package com.m1kllz.ecommerce.product.dto;

import java.math.BigDecimal;

public record ProductResponse(String id, String name, String description,
                String skuCode, BigDecimal price) {
}

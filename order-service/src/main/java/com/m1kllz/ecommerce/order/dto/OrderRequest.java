package com.m1kllz.ecommerce.order.dto;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        Long id,
        String orderNumber,
        @NotBlank(message = "SKU code is required") String skuCode,
        @NotNull(message = "Price is required") @Positive(message = "Price must be positive") BigDecimal price,
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") Integer quantity,
        @Valid UserDetails userDetails) {

    public record UserDetails(
            @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName) {
    }
}

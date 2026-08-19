package com.m1kllz.ecommerce.inventory.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.m1kllz.ecommerce.inventory.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public boolean isInStock(
            @RequestParam @NotBlank String skuCode,
            @RequestParam @Min(1) Integer quantity) {
        return inventoryService.isInStock(skuCode, quantity);
    }
}

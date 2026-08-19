package com.m1kllz.ecommerce.inventory.service;

public interface InventoryService {
    boolean isInStock(String skuCode, Integer quantity);
}

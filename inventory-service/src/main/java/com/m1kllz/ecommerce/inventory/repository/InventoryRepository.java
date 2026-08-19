package com.m1kllz.ecommerce.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.m1kllz.ecommerce.inventory.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(String skuCode, int quantity);
}

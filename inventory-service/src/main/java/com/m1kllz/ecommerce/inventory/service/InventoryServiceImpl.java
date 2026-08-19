package com.m1kllz.ecommerce.inventory.service;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.m1kllz.ecommerce.inventory.repository.InventoryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    @Observed(name = "inventory.isInStock")
    public boolean isInStock(String skuCode, Integer quantity) {
        boolean inStock = inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual(skuCode, quantity);
        log.debug("Stock check skuCode={} quantity={} inStock={}", skuCode, quantity, inStock);
        return inStock;
    }
}

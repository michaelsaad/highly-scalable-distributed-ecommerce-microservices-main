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

    @Override
    @Transactional
    @Observed(name = "inventory.decrement")
    public boolean decrementStock(String skuCode, Integer quantity) {
        int updated = inventoryRepository.decrementQuantity(skuCode, quantity);
        log.info("Decrement stock skuCode={} quantity={} success={}", skuCode, quantity, updated > 0);
        return updated > 0;
    }

    @Override
    @Transactional
    @Observed(name = "inventory.restore")
    public void restoreStock(String skuCode, Integer quantity) {
        inventoryRepository.restoreQuantity(skuCode, quantity);
        log.info("Restored stock skuCode={} quantity={}", skuCode, quantity);
    }
}

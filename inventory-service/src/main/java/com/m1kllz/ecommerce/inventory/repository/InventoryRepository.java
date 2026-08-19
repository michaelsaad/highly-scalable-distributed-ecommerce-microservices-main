package com.m1kllz.ecommerce.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.m1kllz.ecommerce.inventory.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(String skuCode, int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :qty WHERE i.skuCode = :sku AND i.quantity >= :qty")
    int decrementQuantity(@Param("sku") String skuCode, @Param("qty") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :qty WHERE i.skuCode = :sku")
    int restoreQuantity(@Param("sku") String skuCode, @Param("qty") int quantity);
}

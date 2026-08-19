package com.m1kllz.ecommerce.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.m1kllz.ecommerce.order.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    void deleteByOrderNumber(String orderNumber);
}

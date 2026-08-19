package com.m1kllz.ecommerce.order.exception;

public class ProductOutOfStockException extends RuntimeException {
    public ProductOutOfStockException(String msg) {
        super(msg);
    }
}

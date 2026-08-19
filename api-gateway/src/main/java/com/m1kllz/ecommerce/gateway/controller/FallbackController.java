package com.m1kllz.ecommerce.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/products")
    public Mono<String> productFallback() {
        return Mono.just("Product Service is unavailable");
    }

    @RequestMapping("/orders")
    public Mono<String> orderFallback() {
        return Mono.just("Order Service is unavailable");
    }

    @RequestMapping("/inventory")
    public Mono<String> inventoryFallback() {
        return Mono.just("Inventory Service is unavailable");
    }
}

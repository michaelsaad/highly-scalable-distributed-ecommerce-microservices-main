package com.m1kllz.ecommerce.order.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.m1kllz.ecommerce.order.exception.InventoryUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

        private final WebClient webClient;

        @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
        @Retry(name = "inventory")
        public boolean isInStock(String skuCode, int quantity) {
                log.info("Checking inventory for skuCode: {} and quantity: {}", skuCode, quantity);

                try {
                        Boolean result = webClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path("/api/inventory")
                                                        .queryParam("skuCode", skuCode)
                                                        .queryParam("quantity", quantity)
                                                        .build())
                                        .retrieve()
                                        .bodyToMono(Boolean.class)
                                        .block(); // makes it synchronous

                        return Boolean.TRUE.equals(result);

                } catch (RuntimeException ex) {
                        log.error("Network or runtime error during inventory check: {}", ex.getMessage());
                        throw ex; // Rethrow so Resilience4j annotations catch it
                }
        }

        @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
        @Retry(name = "inventory")
        public boolean decrementStock(String skuCode, int quantity) {
                log.info("Decrementing inventory for skuCode: {} quantity: {}", skuCode, quantity);
                Boolean result = webClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/inventory/decrement")
                                                .queryParam("skuCode", skuCode)
                                                .queryParam("quantity", quantity)
                                                .build())
                                .retrieve()
                                .bodyToMono(Boolean.class)
                                .block();
                return Boolean.TRUE.equals(result);
        }

        @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryVoidFallback")
        @Retry(name = "inventory")
        public void restoreStock(String skuCode, int quantity) {
                log.info("Restoring inventory for skuCode: {} quantity: {}", skuCode, quantity);
                webClient.post()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/inventory/restore")
                                                .queryParam("skuCode", skuCode)
                                                .queryParam("quantity", quantity)
                                                .build())
                                .retrieve()
                                .toBodilessEntity()
                                .block();
        }

        public boolean inventoryFallback(String skuCode, int quantity, Throwable ex) {
                log.error("Inventory service fallback executed. Root cause: {}",
                                ex != null ? ex.getMessage() : "Unknown");
                throw new InventoryUnavailableException("Inventory service unavailable. Please try later.");
        }

        public void inventoryVoidFallback(String skuCode, int quantity, Throwable ex) {
                log.error("Inventory service fallback (void) executed. Root cause: {}",
                                ex != null ? ex.getMessage() : "Unknown");
                throw new InventoryUnavailableException("Inventory service unavailable. Please try later.");
        }
}

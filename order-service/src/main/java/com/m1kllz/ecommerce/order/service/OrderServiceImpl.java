package com.m1kllz.ecommerce.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.m1kllz.ecommerce.order.client.InventoryClient;
import com.m1kllz.ecommerce.order.dto.OrderRequest;
import com.m1kllz.ecommerce.order.dto.OrderResponse;
import com.m1kllz.ecommerce.order.event.OrderPlacedEvent;
import com.m1kllz.ecommerce.order.exception.OrderNotFoundException;
import com.m1kllz.ecommerce.order.exception.ProductOutOfStockException;
import com.m1kllz.ecommerce.order.model.Order;
import com.m1kllz.ecommerce.order.repository.OrderRepository;

import io.micrometer.observation.annotation.Observed;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    @Transactional
    @Observed(name = "order.place")
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        boolean inStock = inventoryClient.isInStock(orderRequest.skuCode(), orderRequest.quantity());
        if (!inStock) {
            throw new ProductOutOfStockException(
                    "Product with SKU Code " + orderRequest.skuCode() + " and quantity " + orderRequest.quantity()
                            + " is not available in inventory.");
        }

        boolean decremented = inventoryClient.decrementStock(orderRequest.skuCode(), orderRequest.quantity());
        if (!decremented) {
            throw new ProductOutOfStockException(
                    "Failed to reserve inventory for SKU " + orderRequest.skuCode() + " quantity " + orderRequest.quantity());
        }

        var order = mapToOrder(orderRequest);
        orderRepository.save(order);
        publishOrderPlaced(order, orderRequest);
        log.info("Order placed orderNumber={} skuCode={} quantity={}",
                order.getOrderNumber(), order.getSkuCode(), order.getQuantity());
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToOrderResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::mapToOrderResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    @Transactional
    public void cancelOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        orderRepository.deleteByOrderNumber(orderNumber);
        inventoryClient.restoreStock(order.getSkuCode(), order.getQuantity());
        log.info("Order cancelled orderNumber={}, restored stock skuCode={} quantity={}",
                orderNumber, order.getSkuCode(), order.getQuantity());
    }

    private void publishOrderPlaced(Order order, OrderRequest orderRequest) {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderNumber(order.getOrderNumber());
        if (orderRequest.userDetails() != null) {
            event.setEmail(orderRequest.userDetails().email());
            event.setFirstName(orderRequest.userDetails().firstName());
            event.setLastName(orderRequest.userDetails().lastName());
        } else {
            event.setEmail("");
            event.setFirstName("");
            event.setLastName("");
        }

        String topic = "order-placed";
        try {
            kafkaTemplate.send(topic, order.getOrderNumber(), event).get();
            log.info("Published OrderPlacedEvent topic={} orderNumber={}", topic, order.getOrderNumber());
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish OrderPlacedEvent for order " + order.getOrderNumber(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while publishing OrderPlacedEvent for order " + order.getOrderNumber(), e);
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getSkuCode(), order.getPrice(),
                order.getQuantity());
    }

    // TODO: Price is taken from the client request and not verified against the product catalog.
    //  Add a product-service client to look up the authoritative price by SKU before persisting.
    private static Order mapToOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
        order.setQuantity(orderRequest.quantity());
        order.setSkuCode(orderRequest.skuCode());
        return order;
    }
}

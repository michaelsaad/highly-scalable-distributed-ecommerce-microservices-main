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
        if (!orderRepository.existsByOrderNumber(orderNumber)) {
            throw new OrderNotFoundException(orderNumber);
        }
        orderRepository.deleteByOrderNumber(orderNumber);
        log.info("Order cancelled orderNumber={}", orderNumber);
    }

    private void publishOrderPlaced(Order order, OrderRequest orderRequest) {
        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setOrderNumber(order.getOrderNumber());
        if (orderRequest.userDetails() != null) {
            event.setEmail(orderRequest.userDetails().email());
            event.setFirstName(orderRequest.userDetails().firstName());
            event.setLastName(orderRequest.userDetails().lastName());
        }

        String topic = "order-placed";
        kafkaTemplate.send(topic, order.getOrderNumber(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published OrderPlacedEvent topic={} orderNumber={}", topic, order.getOrderNumber());
                    } else {
                        log.error("Failed to publish OrderPlacedEvent topic={} orderNumber={}",
                                topic, order.getOrderNumber(), ex);
                    }
                });
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getSkuCode(), order.getPrice(),
                order.getQuantity());
    }

    private static Order mapToOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setPrice(orderRequest.price().multiply(BigDecimal.valueOf(orderRequest.quantity())));
        order.setQuantity(orderRequest.quantity());
        order.setSkuCode(orderRequest.skuCode());
        return order;
    }
}

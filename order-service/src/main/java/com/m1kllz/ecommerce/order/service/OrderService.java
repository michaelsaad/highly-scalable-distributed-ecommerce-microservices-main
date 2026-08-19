package com.m1kllz.ecommerce.order.service;

import com.m1kllz.ecommerce.order.dto.OrderRequest;
import com.m1kllz.ecommerce.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest orderRequest);

    List<OrderResponse> getAllOrders();

    OrderResponse getOrderDetail(String orderNumber);

    void cancelOrder(String orderNumber);
}

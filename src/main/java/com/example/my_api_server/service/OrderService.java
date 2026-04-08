package com.example.my_api_server.service;

import com.example.my_api_server.entity.Order;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto create(OrderCreateDto dto);

    OrderResponseDto tracking(Long orderId);

    Order createOrderProduct(Order order, List<Product> products, OrderCreateDto dto);
}

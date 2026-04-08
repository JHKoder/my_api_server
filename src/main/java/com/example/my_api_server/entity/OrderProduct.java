package com.example.my_api_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "order_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    private Long number;


    private OrderProduct(Order order, Long orderCount, Product product) {
        this.product = product;
        this.order = order;
        this.number = orderCount;
    }

    public static OrderProduct ofCreate(Order order, Long orderCount, Product product) {
        return new OrderProduct(order, orderCount, product);
    }
}

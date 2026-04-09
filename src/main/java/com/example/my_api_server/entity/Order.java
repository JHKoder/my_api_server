package com.example.my_api_server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderProduct> orderProducts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member buyer;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private LocalDateTime orderTime;

    public Order(Member member, OrderStatus orderStatus, LocalDateTime orderTime) {
        this.buyer = member;
        this.orderStatus = orderStatus;
        this.orderTime = orderTime;
    }

    public static Order createOrder(Member member, LocalDateTime orderTime) {
        return new Order(member, OrderStatus.PENDING, orderTime);
    }

    public void addOrderProduct(Long orderCount, Product product) {
        this.orderProducts.add(OrderProduct.ofCreate(this, orderCount, product));
    }

    public void stateComplete() {
        this.orderStatus = OrderStatus.COMPLETED;
    }
}

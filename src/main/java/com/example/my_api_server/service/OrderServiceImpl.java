package com.example.my_api_server.service;

import com.example.my_api_server.entity.Member;
import com.example.my_api_server.entity.Order;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Slf4j
@Service
@Profile("dev")
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepo orderRepo;
    private final MemberDBRepo memberRepo;
    private final ProductRepo productRepo;

    @Transactional
    public OrderResponseDto tracking(Long orderId) {
        return OrderResponseDto.ofOrderSuccess(findOrderById(orderId));
    }

    @Transactional
    public OrderResponseDto create(OrderCreateDto dto) {
        Member member = findMemberById(dto.getMemberId());
        Order order = Order.createOrder(member, dto.getOrderTim());

        List<Product> products = productRepo.findAllByIdsWithXLock(dto.getProductIds());
        verifiedProductIds(products,dto.getProductIds());

        return OrderResponseDto.ofOrderSuccess(createOrderProduct(order, products, dto));
    }

    public Order createOrderProduct(Order order, List<Product> products, OrderCreateDto dto) {
        products.forEach(product -> {
            Long count = dto.findByIdProductCount(product.getId());
            product.buyProductWithStock(count);
            order.addOrderProduct(count, product);
        });

        order.stateComplete();
        return orderRepo.save(order);
    }

    private void verifiedProductIds(List<Product> products, List<Long> requestProductIds) {
        if(products.isEmpty() || !Objects.equals(products.size(),requestProductIds.size())){
            throw new RuntimeException("존재하지 않는 상품 입력");
        }
    }
    private Member findMemberById(Long memberId) {
        return memberRepo.findById(memberId).orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));
    }

    private Order findOrderById(Long orderId) {
        return orderRepo.findById(orderId).orElseThrow(() -> new RuntimeException("주문이 존재하지 않습니다."));
    }
}
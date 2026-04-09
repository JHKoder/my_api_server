package com.example.my_api_server.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class OrderCreateDto {
    @NotNull(message = "구매자 번호는 필수입니다.")
    private final Long memberId;
    @NotEmpty(message = "상품은 최소 1개 이상 입력해야합니다.")
    private final List<OrderCreateItemDto> products;
    private final LocalDateTime orderTim;

    public OrderCreateDto(Long memberId, List<OrderCreateItemDto> products) {
        this(memberId, products, LocalDateTime.now());
    }

    @JsonCreator
    public OrderCreateDto(Long memberId, List<OrderCreateItemDto> products, LocalDateTime orderTim) {
        this.memberId = memberId;
        this.products = products;
        this.orderTim = orderTim == null ? LocalDateTime.now() : orderTim;
        validateProductIdDuplicate();
    }

    public List<Long> getProductIds() {
        return new ArrayList<>(products.stream().map(product -> product.id).toList());
    }

    public List<Long> getProductCounts() {
        return new ArrayList<>(products.stream().map(product -> product.count).toList());
    }

    public static List<OrderCreateItemDto> orderCreateDtoProducts(List<Long> productIds, List<Long> counts) {
        List<OrderCreateItemDto> result = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            result.add(new OrderCreateItemDto(productIds.get(i), counts.get(i)));
        }
        return result;
    }

    public Long findByIdProductCount(Long productId) {
        return products.stream().filter(product -> product.id.equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("매칭된 상품 id가없습니다."))
                .count();
    }

    public void validateProductIdDuplicate() {
        Set<Long> seen = new HashSet<>();
        boolean isUnique = products.stream()
                .map(OrderCreateItemDto::id)
                .allMatch(seen::add);
        if (!isUnique) {
            throw new RuntimeException("중복된 상품ID가 입력됨");
        }
    }

    public record OrderCreateItemDto(
            @NotNull(message = "상품 ID는 필수입니다.")
            @Min(value = 1, message = "상품 ID는 1 이상이어야 합니다.") Long id,

            @NotNull(message = "주문 수량은 필수입니다.")
            @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.") Long count) {
    }
}
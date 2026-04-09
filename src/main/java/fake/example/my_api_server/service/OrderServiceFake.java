package fake.example.my_api_server.service;


import com.example.my_api_server.entity.Member;
import com.example.my_api_server.entity.Order;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.OrderService;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
@Profile("test")
@AllArgsConstructor
public class OrderServiceFake implements OrderService {
    private final OrderRepo orderRepo;
    private final MemberDBRepo memberRepo;
    private final ProductRepo productRepo;

    @Transactional
    public OrderResponseDto tracking(Long orderId) {
        return null;
    }

    @Transactional(propagation = REQUIRES_NEW)
    @Retryable(includes = ObjectOptimisticLockingFailureException.class, maxRetries = 3)
    public OrderResponseDto create(OrderCreateDto dto) {
        Order order = Order.createOrder(findMemberById(dto.getMemberId()), dto.getOrderTim());

        List<Product> product = productRepo.findAllById(dto.getProductIds());

        return OrderResponseDto.ofOrderSuccess(createOrderProduct(order, product, dto));
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

    private Member findMemberById(Long memberId) {
        return memberRepo.findById(memberId).orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));
    }
}

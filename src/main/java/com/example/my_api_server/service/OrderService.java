package com.example.my_api_server.service;

import com.example.my_api_server.entity.Member;
import com.example.my_api_server.entity.Order;
import com.example.my_api_server.entity.OrderProduct;
import com.example.my_api_server.entity.OrderStatus;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.event.MemberSignUpEvent;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepo orderRepo;
    private final MemberDBRepo memberRepo;
    private final ProductRepo productRepo;
    private final MemberPointService memberPointService;

    //주문 생성
    //Tomcat의 wt-1
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto dto) {
        Member member = memberRepo.findById(dto.memberId()).orElseThrow();
        LocalDateTime orderTime = LocalDateTime.now();

        Order order = Order.builder()
            .buyer(member)
            .orderStatus(OrderStatus.PENDING)
            .orderTime(orderTime)
            .build();

        //jpql,quierydsl, jdbc template

        //상품 id들을 통해서 상품들을 조회할겁니다.(N번 조회)
        //쿼리가 N번되죠?, 1번으로 줄일 수 있을까요?

//        List<Product> products = dto.productId().stream()
//            .map((pId) -> productRepo.findById(pId).orElseThrow())
//            .toList();

        List<Product> products = productRepo.findAllById(dto.productId()); //IN 쿼리
        List<OrderProduct> orderProducts = IntStream.range(0, dto.count().size())
            .mapToObj(idx -> {
                //재고에대해서 차감을 해야한다.(음수 처리x)
                Product product = products.get(idx);

                //현재 재고에서 주문재고 감했을때 음수이면 <0 예외터트린다!(주문못하게 막는다!)
                if (product.getStock() - dto.count().get(idx) < 0) {
                    throw new RuntimeException("재고가 음수이니 주문 할 수 없습니다!");
                }

                //재고 감소
                //update product set stock = stock - 1 where pk =1;(더티체킹, 스냅샷 값을 비교한다!)
                product.decreaseStock(dto.count().get(idx));

                return OrderProduct.builder()
                    .order(order)
                    .number(dto.count().get(idx)) //product에 맞는 주문개수를 찾는다!
                    .product(products.get(idx))
                    .build();
            })
            .toList();

//        Map<Product, Long> productCountMap = new HashMap<>();
//        for (int i = 0; i < dto.count().size(); i++) {
//            productCountMap.put(products.get(i), dto.count().get(i));
//        }
//
//        //상품들을 찾으니 상품 개수 만큼 orderProduct를 생성해주면 됩니다!
//        List<OrderProduct> orderProducts = products.stream()
//            .map(p -> OrderProduct.builder()
//                .order(order)
//                .number(productCountMap.get(p)) //product에 맞는 주문개수를 찾는다!
//                .product(p)
//                .build()
//            )
//            .toList();

        //OrderProduct 생명주기 Order와 Sync 생성 완료(Order 저장되면 OP 저장)
        order.addOrderProducts(orderProducts);

        //order save를 하기전에는 영속화x
        Order savedOrder = orderRepo.save(order); //wt1- 영속성 컨텍스트가 1개 관리
        //order save를 한 후 에는 영속화(내자식으로 관리를 하겟다)

        //Entity -> Dto로 변환
        OrderResponseDto orderResponseDto = OrderResponseDto.of(
            savedOrder.getOrderTime(),
            OrderStatus.COMPLETED,
            true);

        String s = memberPointService.sendEmail(new MemberSignUpEvent(1L, "fds", "fds"));
        
        //업데이트를 한번에 1억개 -> 10000개씩 쪼개서 10000단위(청크) 약간의 텀을두거나 잘게쪼개서 update, DB입장에서는 부담이 조금 적어기겟죠(JPA Batch, JDBC BATCH)

        return orderResponseDto;
    }


    //낙관락 적용
    @Retryable(
        includes = ObjectOptimisticLockingFailureException.class,
        maxRetries = 3
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderResponseDto createOrderWithOptLock(OrderCreateDto dto) {
        log.info("retry");
        Member member = memberRepo.findById(dto.memberId()).orElseThrow();
        LocalDateTime orderTime = LocalDateTime.now();

        Order order = Order.builder()
            .buyer(member)
            .orderStatus(OrderStatus.PENDING)
            .orderTime(orderTime)
            .build();

        List<Product> products = productRepo.findAllById(dto.productId()); //LOCK IN 쿼리
        List<OrderProduct> orderProducts = IntStream.range(0, dto.count().size())
            .mapToObj(idx -> {
                //재고에대해서 차감을 해야한다.(음수 처리x)
                Product product = products.get(idx);

                //현재 재고에서 주문재고 감했을때 음수이면 <0 예외터트린다!(주문못하게 막는다!)
                if (product.getStock() - dto.count().get(idx) < 0) {
                    throw new RuntimeException("재고가 음수이니 주문 할 수 없습니다!");
                }

                //재고 감소
                product.decreaseStock(dto.count().get(idx));

                return OrderProduct.builder()
                    .order(order)
                    .number(dto.count().get(idx)) //product에 맞는 주문개수를 찾는다!
                    .product(products.get(idx))
                    .build();
            })
            .toList();

        order.addOrderProducts(orderProducts);
        Order savedOrder = orderRepo.save(order);

        OrderResponseDto orderResponseDto = OrderResponseDto.of(
            savedOrder.getOrderTime(),
            OrderStatus.COMPLETED,
            true);

        return orderResponseDto;
    }


    //비관락 적용
    @Transactional
    public OrderResponseDto createOrderWithXLock(OrderCreateDto dto) {
        Member member = memberRepo.findById(dto.memberId()).orElseThrow();
        LocalDateTime orderTime = LocalDateTime.now();

        Order order = Order.builder()
            .buyer(member)
            .orderStatus(OrderStatus.PENDING)
            .orderTime(orderTime)
            .build();

        List<Product> products = productRepo.findAllByIdsWithLock(dto.productId()); //LOCK IN 쿼리
        List<OrderProduct> orderProducts = IntStream.range(0, dto.count().size())
            .mapToObj(idx -> {
                //재고에대해서 차감을 해야한다.(음수 처리x)
                Product product = products.get(idx);

                //현재 재고에서 주문재고 감했을때 음수이면 <0 예외터트린다!(주문못하게 막는다!)
                if (product.getStock() - dto.count().get(idx) < 0) {
                    throw new RuntimeException("재고가 음수이니 주문 할 수 없습니다!");
                }

                //재고 감소
                product.decreaseStock(dto.count().get(idx));

                return OrderProduct.builder()
                    .order(order)
                    .number(dto.count().get(idx)) //product에 맞는 주문개수를 찾는다!
                    .product(products.get(idx))
                    .build();
            })
            .toList();

        order.addOrderProducts(orderProducts);
        Order savedOrder = orderRepo.save(order);

        OrderResponseDto orderResponseDto = OrderResponseDto.of(
            savedOrder.getOrderTime(),
            OrderStatus.COMPLETED,
            true);

        return orderResponseDto;
    }

    //주문 조회
    //사용자가 최종 주문 확정을 누르면 주문 상태를 확정으로 바꾼다.(주문 수정)

    /**
     * JPA는 내부적으로 캐시(중간 지점의 미니 창고) 매커니즘 - 내부에 1차캐시, 2차캐시 - 1차캐시 내부적으로 영속화(내 자식으로 만들겟다) -
     * readonly=true시 내부 하이버네이트 동작원리가 간소화가된다.(더테 체킹 x)
     */
    @Transactional(readOnly = true)
    public OrderResponseDto findOrder(Long orderId) {
        Order order = orderRepo.findById(orderId).orElseThrow(); //ID로 주문 조회

        //주문 조회 -> DTO 변환
        OrderResponseDto orderResponseDto = OrderResponseDto.of(
            order.getOrderTime(),
            order.getOrderStatus(),
            true);

        return orderResponseDto;
    }
}

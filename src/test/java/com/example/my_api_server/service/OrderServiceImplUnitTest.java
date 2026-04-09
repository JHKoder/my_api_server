package com.example.my_api_server.service;

import com.example.my_api_server.config.TestContainerConfig;
import com.example.my_api_server.entity.*;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderService 단위테스트
 */
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class) //Mockito 활성화
class OrderServiceImplUnitTest {

    @Mock //가짜 객체 생성
    ProductRepo productRepo;

    @Mock //가짜 객체 생성
    MemberDBRepo memberDBRepo;

    @Mock //가짜 객체 생성
    OrderRepo orderRepo;

    @InjectMocks //실제 테스트할 대상 클래스(Mock 객체를 자동으로 주입받는다)
    OrderServiceImpl orderService;

    //초기 데이터용 클래스 객체
    InitData initData;

    OrderCreateDto orderCreateDto;

    //테스트 실행하기전에 이 메서드를 실행해줍니다.
    @BeforeEach
    public void init() {
        initData = new InitData();
        initData.memberId = 1L;
        initData.productIds = List.of(1L, 2L);
        initData.counts = List.of(1L, 2L);

        initData.product1 = Product.builder()
                .id(1L)
                .productNumber("TEST1")
                .productName("티셔츠 1")
                .productType(ProductType.CLOTHES)
                .price(1000L)
                .stock(2L)
                .build();

        initData.product2 = Product.builder()
                .id(2L)
                .productNumber("TEST2")
                .productName("티셔츠 2")
                .productType(ProductType.CLOTHES)
                .price(2000L)
                .stock(4L)
                .build();

        initData.member = Member.builder()
                .email("test1@gmail.com")
                .password("1234")
                .build();


        orderCreateDto = new OrderCreateDto(
                initData.memberId,
                OrderCreateDto.orderCreateDtoProducts(initData.productIds, initData.counts));
    }

    @Test
    @DisplayName("[HAPPY]주문 요청이 정상적으로 잘 등록된다")
    public void createSuccess() {
        when(memberDBRepo.findById(initData.memberId)).thenReturn(Optional.of(initData.member));
        when(productRepo.findAllByIdsWithXLock(initData.productIds)).thenReturn(
                List.of(initData.product1, initData.product2));
        when(orderRepo.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0));

        OrderResponseDto dto = orderService.create(orderCreateDto);
        ArgumentCaptor<Order> capture = ArgumentCaptor.forClass(Order.class);

        verify(orderRepo).save(capture.capture()); //orderRepo save()가 호출되는지 확인
        assertThat(dto.isSuccess()).isTrue(); //성공 여부 검증
        assertThat(dto.getOrderStatus()).isEqualTo(OrderStatus.COMPLETED); //주문 상태 검증
    }

    @Test
    @DisplayName("[Exception]주문 요청시 재고 부족하면 예외 처리가 정상 동작한다")
    public void productStockValid() {
        //given(when절에 필요한 데이터들 생성)
        //공통적으로 데이터를 사용하지않는 케이스(재고 관련해서 조금 수정필요하다)
        Long memberId = 1L;
        List<Long> productIds = List.of(1L, 2L);
        List<Long> counts = List.of(10L, 20L);
        List<OrderCreateDto.OrderCreateItemDto> orderCreateItemDtoProducts = OrderCreateDto.orderCreateDtoProducts(productIds, counts);

        Product product1 = Product.builder()
                .id(productIds.get(0))
                .productNumber("TEST1")
                .productName("티셔츠 1")
                .productType(ProductType.CLOTHES)
                .price(1000L)
                .stock(1L)
                .build();

        Product product2 = Product.builder()
                .id(productIds.get(1))
                .productNumber("TEST2")
                .productName("티셔츠 2")
                .productType(ProductType.CLOTHES)
                .price(2000L)
                .stock(2L)
                .build();

        Member member = Member.builder()
                .id(memberId)
                .email("test1@gmail.com")
                .password("1234")
                .build();

        OrderCreateDto createDto = new OrderCreateDto(memberId,orderCreateItemDtoProducts,LocalDateTime.now());

        //DB와 통신하지 않게 우리가 proxy처럼 임의로 실행 시켜줘야한다.
        when(memberDBRepo.findById(memberId))
                .thenReturn(Optional.of(member));
        when(productRepo.findAllByIdsWithXLock(productIds))
                .thenReturn(List.of(product1, product2));

        //when(테스트할 메서드)
        //then(값 검증)
        assertThatThrownBy(() -> orderService.create(createDto))
                .isInstanceOf(RuntimeException.class)//해당 예외 클래스가 어떤건지 지정합니다.
                .hasMessage("재고가 음수이니 주문 할 수 없습니다!"); //해당 예외 메시지가 어떤건지 지정
    }

    //    @Test
    @DisplayName("[Exception]주문 시간 날짜 오류 테스트")
    public void orderTimeException() {
        //given(when절에 필요한 데이터들 생성)

        //DB와 통신하지 않게 우리가 proxy처럼 임의로 실행 시켜줘야한다.
        when(productRepo.findAllById(initData.productIds)).thenReturn(
                List.of(initData.product1, initData.product2));
        when(memberDBRepo.findById(initData.memberId)).thenReturn(Optional.of(initData.member));

        //when(테스트할 메서드)
        //테스트가 시간여부에따라서 달라진다는거죠(이게 좋을까요?)
        LocalDateTime orderTime = LocalDateTime.parse("2026-04-03T13:00:00");
        OrderResponseDto dto = orderService.create(orderCreateDto);

        //then(값 검증)
        //Cannot invoke "com.example.my_api_server.entity.Order.getOrderTime()" because "savedOrder" is null
        assertThat(dto).isNull();
    }

    //테스트 용 초기데이터 클래스
    public class InitData {

        public Long memberId;
        public List<Long> productIds;
        public List<Long> counts;

        public Product product1;
        public Product product2;
        public Member member;

        public void init() {
            //내부초기화
        }
    }
}
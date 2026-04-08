package com.example.my_api_server.service;

import com.example.my_api_server.config.TestContainerConfig;
import com.example.my_api_server.entity.Member;
import com.example.my_api_server.entity.Order;
import com.example.my_api_server.entity.Product;
import com.example.my_api_server.repo.MemberDBRepo;
import com.example.my_api_server.repo.OrderProductRepo;
import com.example.my_api_server.repo.OrderRepo;
import com.example.my_api_server.repo.ProductRepo;
import com.example.my_api_server.service.dto.OrderCreateDto;
import com.example.my_api_server.service.dto.OrderResponseDto;
import fixture.MemberFixture;
import fixture.ProductFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainerConfig.class)
public class OrderServiceImplIntegrationTest {
    @Autowired
    private OrderServiceImpl orderServiceImpl;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private MemberDBRepo memberDBRepo;
    @Autowired
    private OrderProductRepo orderProductRepo;
    @Autowired
    private OrderService orderService;

    private Member member;

    @BeforeEach
    public void setup() {
        orderProductRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        orderRepo.deleteAllInBatch();
        memberDBRepo.deleteAllInBatch();

        member = getSavedMember("1234");
    }

    @Nested
    @DisplayName("주문 생성 TC")
    class OrderCreateTest {


        @ParameterizedTest
        @CsvSource({"1", "5", "10"})
        @DisplayName("주문 생성 개수 조회 테스트")
        void createOrderSelect(Long createCount) {
            List<Long> counts = List.of(1L, 1L);
            List<Product> products = getProducts(createCount);
            OrderCreateDto dto = new OrderCreateDto(member.getId(), OrderCreateDto.orderCreateDtoProducts(getProductIds(products), counts));

            for (int i = 0; i < createCount; i++) {
                orderService.createOrderProduct(Order.createOrder(member, dto.getOrderTim()), products, dto);
            }

            assertThat(orderRepo.findByBuyerId(member.getId()).size()).isEqualTo((long) createCount);
        }

        @Test
        @DisplayName("존재하지 않는 상품에 대한 예외 테스트")
        void exception_testing_for_non_existent_goods() {
            OrderCreateDto dto = new OrderCreateDto(member.getId(), OrderCreateDto.orderCreateDtoProducts(List.of(1022L, 2211L), List.of(1L, 1L)));

            assertThatThrownBy(() -> orderServiceImpl.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("존재하지 않는 상품 입력");
        }

        @Test
        @DisplayName("주문 생성 시 DB에 저장되고 주문시간이 Null이 아니다.")
        public void createOrderPersistAndReturn() {
            //given
            List<Long> counts = List.of(1L, 1L);
            Member savedMember = getSavedMember("1234"); //멤버 저장
            List<Product> products = getProducts(); //상품 저장
            List<Long> productIds = getProductIds(products); //productIds 추출 작업

            OrderCreateDto createDto = new OrderCreateDto(savedMember.getId(), OrderCreateDto.orderCreateDtoProducts(productIds, counts));

            //when
            OrderResponseDto retDto = orderServiceImpl.create(createDto);

            //then
            assertThat(retDto.getOrderCompletedTime()).isNotNull();
        }

        @Test
        @DisplayName("주문 생성시 재고가 정상적으로 차감이 된다.")
        public void createOrderStockDecreaseSuccess() {
            //givenx
            List<Product> products = getProducts();
            List<Long> productIds = getProductIds(products);
            OrderCreateDto createDto = new OrderCreateDto(member.getId(), OrderCreateDto.orderCreateDtoProducts(productIds, List.of(1L, 1L)));

            //when
            orderServiceImpl.create(createDto);

            //then
            List<Product> resultProducts = productRepo.findAllById(productIds);

            //주문 재고 검증 로직
            for (int i = 0; i < products.size(); i++) {
                Product beforeProduct = products.get(i);
                Product nowProduct = resultProducts.stream().filter(p -> p.getId().equals(beforeProduct.getId())).findFirst()
                        .get();
                Long orderStock = createDto.findByIdProductCount(nowProduct.getId());

                System.out.println(beforeProduct.getStock() + " - " + orderStock + " = " + nowProduct.getStock());
                assertThat(beforeProduct.getStock() - orderStock).isEqualTo(nowProduct.getStock());
            }
        }


        @Test
        @DisplayName("주문 생성시 재고가 부족하면 예외가 정상 동작한다.")
        public void createOrderStockValidation() {
            List<Long> counts = List.of(10L, 10L);
            Member savedMember = getSavedMember("1234");
            List<Product> products = getProducts();
            List<Long> productIds = getProductIds(products);

            OrderCreateDto createDto = new OrderCreateDto(savedMember.getId(), OrderCreateDto.orderCreateDtoProducts(productIds, counts));

            assertThatThrownBy(() -> orderServiceImpl.create(createDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("재고가 음수이니 주문 할 수 없습니다!");
        }


        @Test
        @DisplayName("주문 생성시 상품 개수를 조회한다.")
        public void OrderCreateTest() {
            //given
            List<Long> counts = List.of(1L, 1L);
            List<Product> products = getProducts(); //상품 저장
            List<Long> productIds = getProductIds(products); //productIds 추출 작업

            OrderCreateDto createDto = new OrderCreateDto(member.getId(), OrderCreateDto.orderCreateDtoProducts(productIds, counts));

            //when
            orderServiceImpl.create(createDto);

            //then
            List<Product> productList = productRepo.findAll();
            assertThat(productList.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("주문과 연관된 도메인 예외 TC")
    class OrderRelatedExceptionTest {

        @Test
        @DisplayName("주문 시 회원이 존재하지 않으면 예외가 발생한다.")
        public void validateMemberWhenCreateOrder() {
            //given
            List<Long> counts = List.of(1L, 1L);
            List<Product> products = getProducts();
            List<Long> productIds = getProductIds(products);
            OrderCreateDto createDto = new OrderCreateDto(member.getId() + 1L, OrderCreateDto.orderCreateDtoProducts(productIds, counts));

            //when, then
            assertThatThrownBy(() -> orderServiceImpl.create(createDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("회원이 존재하지 않습니다.");
        }
    }

    private OrderCreateDto getOrderCreateDto(Member member, List<Long> product) {
        return new OrderCreateDto(member.getId(),
                OrderCreateDto.orderCreateDtoProducts(product, List.of(1L, 1L)));
    }

    private List<Long> getProductIds(List<Product> products) {
        return products.stream()
                .map(Product::getId)
                .toList();
    }

    private Member getSavedMember(String password) {
        return memberDBRepo.save(MemberFixture
                .defaultMember()
                .password(password)
                .build()
        );
    }

    private List<Product> getProducts() {
        return productRepo.saveAll(ProductFixture
                .defaultProducts()
        );
    }

    private List<Product> getProducts(Long stock) {
        return productRepo.saveAll(ProductFixture
                .defaultProductsStock(stock)
        );
    }
}

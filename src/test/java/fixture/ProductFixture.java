package fixture;

import com.example.my_api_server.entity.Product;
import com.example.my_api_server.entity.ProductType;

import java.util.List;

public class ProductFixture {
    public static final String NUMBER = "TEST";
    public static final String NAME = "티셔츠";

    public static Product.ProductBuilder defaultProduct() {
        return Product.builder().productType(ProductType.CLOTHES);
    }

    public static List<Product> defaultProductsStock(Long stock) {
        return List.of(
                create(1, 1000L, stock),
                create(2, 2000L, stock)
        );
    }

    public static List<Product> defaultProducts() {
        return List.of(
                create(1, 1000L, 1L),
                create(2, 2000L, 2L)
        );
    }

    private static Product create(int index, Long price, Long stock) {
        return create(null, null, null, price, stock, index);
    }

    private static Product create(Long id, String number, String name, Long price, Long stock, int index) {
        int seq = index == 0 ? 1 : index;
        return Product.builder()
                .id(id)
                .productNumber(number == null ? NUMBER + " " + seq : number)
                .productName(name == null ? NAME + " " + seq : name)
                .productType(ProductType.CLOTHES)
                .price(price == 0L ? 1000L * seq : price)
                .stock(stock == 0L ? 1000L * seq : stock)
                .build();
    }
}

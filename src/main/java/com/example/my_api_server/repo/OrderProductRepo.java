package com.example.my_api_server.repo;

import com.example.my_api_server.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepo extends JpaRepository<OrderProduct, Long> {

}

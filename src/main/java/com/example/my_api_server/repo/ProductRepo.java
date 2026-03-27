package com.example.my_api_server.repo;

import com.example.my_api_server.entity.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {


    //    @Lock(LockModeType.PESSIMISTIC_READ) //FOR_SHARE
    //FOR-UPDATE Lock(PG에서는 자체 최적화로 FOR NO KEY UPDATE, Mysql에서는 for udpate)
    @Lock(LockModeType.PESSIMISTIC_WRITE) //FOR UPDATE
    @Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id")
    List<Product> findAllByIdsWithLock(List<Long> ids);
    //데드락 방지로 인한 동일한 순서로 lock 획득

}

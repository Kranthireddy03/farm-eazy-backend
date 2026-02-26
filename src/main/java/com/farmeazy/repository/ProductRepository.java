package com.farmeazy.repository;

import com.farmeazy.entity.Product;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findBySeller(User seller);
    
    List<Product> findByCategory(String category);
    
    List<Product> findByStatus(String status);
    
    List<Product> findBySellerAndStatus(User seller, String status);
    
    List<Product> findByCategoryAndStatus(String category, String status);
}

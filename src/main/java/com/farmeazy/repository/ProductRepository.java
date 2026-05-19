package com.farmeazy.repository;

import com.farmeazy.entity.Product;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findBySeller(User seller);
    
    List<Product> findByCategory(String category);
    
    List<Product> findByStatus(String status);

    @EntityGraph(attributePaths = {"seller", "mediaFiles"})
    List<Product> findByStatusOrderByCreatedAtDesc(String status);
    
    List<Product> findBySellerAndStatus(User seller, String status);

    @EntityGraph(attributePaths = {"seller", "mediaFiles"})
    List<Product> findBySellerOrderByCreatedAtDesc(User seller);
    
    List<Product> findByCategoryAndStatus(String category, String status);

    @EntityGraph(attributePaths = {"seller", "mediaFiles"})
    List<Product> findByCategoryAndStatusOrderByCreatedAtDesc(String category, String status);

    @EntityGraph(attributePaths = {"seller", "mediaFiles"})
    Optional<Product> findWithDetailsById(Long id);

    Optional<Product> findByIdAndSellerEmail(Long id, String sellerEmail);

    long countByStatus(String status);
}

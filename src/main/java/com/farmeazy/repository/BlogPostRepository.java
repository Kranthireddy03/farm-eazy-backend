package com.farmeazy.repository;

import com.farmeazy.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<BlogPost> findByStatusOrderByPublishedAtDesc(BlogPost.BlogStatus status);
    List<BlogPost> findByStatusAndCategoryIgnoreCaseOrderByPublishedAtDesc(BlogPost.BlogStatus status, String category);
    List<BlogPost> findAllByOrderByUpdatedAtDesc();
    List<BlogPost> findByCreatedByAndSourceOrderByUpdatedAtDesc(String createdBy, String source);
}

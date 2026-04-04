package com.farmeazy.repository;

import com.farmeazy.entity.BlogPostRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRatingRepository extends JpaRepository<BlogPostRating, Long> {
    Optional<BlogPostRating> findByBlogPostIdAndUserId(Long blogPostId, Long userId);
    List<BlogPostRating> findByBlogPostId(Long blogPostId);
}

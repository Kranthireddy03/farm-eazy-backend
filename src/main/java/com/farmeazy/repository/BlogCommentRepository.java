package com.farmeazy.repository;

import com.farmeazy.entity.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {
    List<BlogComment> findByBlogPostIdOrderByCreatedAtAsc(Long blogPostId);
}

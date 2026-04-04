package com.farmeazy.controller;

import com.farmeazy.dto.BlogPostDto;
import com.farmeazy.service.BlogPostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/blog-posts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminBlogPostController {

    private static final Logger logger = LoggerFactory.getLogger(AdminBlogPostController.class);

    @Autowired
    private BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<List<BlogPostDto>> getAll() {
        logger.info("ADMIN_BLOG_GET_ALL");
        return ResponseEntity.ok(blogPostService.getAllPostsForAdmin());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostDto> getById(@PathVariable Long id) {
        logger.info("ADMIN_BLOG_GET_BY_ID id={}", id);
        return ResponseEntity.ok(blogPostService.getPostForAdmin(id));
    }

    @PostMapping
    public ResponseEntity<BlogPostDto> create(@Valid @RequestBody BlogPostDto dto, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_CREATE actor={} title={}", actor, dto != null ? dto.getTitle() : null);
        return ResponseEntity.ok(blogPostService.createPost(dto, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogPostDto> update(@PathVariable Long id, @Valid @RequestBody BlogPostDto dto, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_UPDATE id={} actor={}", id, actor);
        return ResponseEntity.ok(blogPostService.updatePost(id, dto, actor));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<BlogPostDto> publish(@PathVariable Long id, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_PUBLISH id={} actor={}", id, actor);
        return ResponseEntity.ok(blogPostService.publishPost(id, actor));
    }

    @PostMapping("/{id}/submit-approval")
    public ResponseEntity<BlogPostDto> submitApproval(@PathVariable Long id, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_SUBMIT_APPROVAL id={} actor={}", id, actor);
        return ResponseEntity.ok(blogPostService.submitForApproval(id, actor));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BlogPostDto> approve(@PathVariable Long id, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_APPROVE id={} actor={}", id, actor);
        return ResponseEntity.ok(blogPostService.approveAndPublish(id, actor));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<BlogPostDto> unpublish(@PathVariable Long id, Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "system";
        logger.info("ADMIN_BLOG_UNPUBLISH id={} actor={}", id, actor);
        return ResponseEntity.ok(blogPostService.unpublishPost(id, actor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        logger.info("ADMIN_BLOG_DELETE id={}", id);
        blogPostService.deletePost(id);
        return ResponseEntity.ok(Map.of("message", "Blog post deleted"));
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.BlogPostDto;
import com.farmeazy.entity.User;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.BlogPostService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blog-posts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class UserBlogPostController {

    private static final Logger logger = LoggerFactory.getLogger(UserBlogPostController.class);

    @Autowired
    private BlogPostService blogPostService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/submissions/my")
    public ResponseEntity<List<BlogPostDto>> getMySubmissions(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required");
        }

        String actorEmail = authentication.getName();
        logger.info("USER_BLOG_GET_MY_SUBMISSIONS actor={}", actorEmail);
        return ResponseEntity.ok(blogPostService.getUserSubmittedPosts(actorEmail));
    }

    @PostMapping("/submissions")
    public ResponseEntity<BlogPostDto> submitUserBlog(@Valid @RequestBody BlogPostDto dto, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required");
        }

        String actorEmail = authentication.getName();
        User user = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        logger.info("USER_BLOG_SUBMIT actor={} title={}", actorEmail, dto != null ? dto.getTitle() : null);
        BlogPostDto created = blogPostService.submitUserPost(dto, actorEmail, user.getUsername());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{slug}/ratings")
    public ResponseEntity<BlogPostDto> rateBlog(
            @PathVariable String slug,
            @RequestBody java.util.Map<String, Object> body,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required");
        }

        String actorEmail = authentication.getName();
        int rating = Integer.parseInt(String.valueOf(body != null ? body.getOrDefault("rating", 0) : 0));
        logger.info("USER_BLOG_RATE actor={} slug={} rating={}", actorEmail, slug, rating);
        return ResponseEntity.ok(blogPostService.ratePost(slug, actorEmail, rating));
    }

    @GetMapping("/{slug}/comments")
    public ResponseEntity<java.util.List<com.farmeazy.dto.BlogCommentDto>> getComments(@PathVariable String slug) {
        return ResponseEntity.ok(blogPostService.getComments(slug));
    }

    @PostMapping("/{slug}/comments")
    public ResponseEntity<com.farmeazy.dto.BlogCommentDto> addComment(@PathVariable String slug, @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        String content = body != null ? body.getOrDefault("content", "") : "";
        String actor = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            actor = authentication.getName();
        }
        return ResponseEntity.ok(blogPostService.addComment(slug, actor, content));
    }
}

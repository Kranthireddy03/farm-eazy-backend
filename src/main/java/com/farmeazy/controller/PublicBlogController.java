package com.farmeazy.controller;

import com.farmeazy.dto.BlogPostDto;
import com.farmeazy.service.BlogPostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/blog-posts")
@CrossOrigin(origins = {
        "https://farm-eazy.com",
        "https://www.farm-eazy.com",
        "http://localhost:3000",
        "http://localhost:4200",
        "http://localhost:5173"
})
public class PublicBlogController {

    private static final Logger logger = LoggerFactory.getLogger(PublicBlogController.class);

    @Autowired
    private BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<List<BlogPostDto>> getPublicPosts(@RequestParam(required = false) String category) {
        logger.info("PUBLIC_BLOG_GET_POSTS category={}", category);
        return ResponseEntity.ok(blogPostService.getPublicPosts(category));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BlogPostDto> getPublicPost(@PathVariable String slug) {
        logger.info("PUBLIC_BLOG_GET_BY_SLUG slug={}", slug);
        return ResponseEntity.ok(blogPostService.getPublicPostBySlug(slug));
    }
}

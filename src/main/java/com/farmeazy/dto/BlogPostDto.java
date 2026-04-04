package com.farmeazy.dto;

import com.farmeazy.entity.BlogPost;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public class BlogPostDto {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 220, message = "Title must not exceed 220 characters")
    private String title;

    private String slug;

    @NotBlank(message = "Excerpt is required")
    @Size(max = 400, message = "Excerpt must not exceed 400 characters")
    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    @Size(max = 120, message = "Category must not exceed 120 characters")
    private String category;

    private List<String> tags;

    @Size(max = 500, message = "Cover image URL must not exceed 500 characters")
    private String coverImageUrl;

    @NotBlank(message = "Author name is required")
    @Size(max = 150, message = "Author name must not exceed 150 characters")
    private String authorName;

    @Size(max = 80, message = "Source must not exceed 80 characters")
    private String source;

    private BlogPost.BlogStatus status;
    private Integer ratingCount;
    private Double averageRating;
    private OffsetDateTime publishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public BlogPost.BlogStatus getStatus() { return status; }
    public void setStatus(BlogPost.BlogStatus status) { this.status = status; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

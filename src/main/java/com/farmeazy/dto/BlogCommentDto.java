package com.farmeazy.dto;

import com.farmeazy.entity.BlogComment;

import java.time.OffsetDateTime;

public class BlogCommentDto {
    private Long id;
    private String authorName;
    private String content;
    private OffsetDateTime createdAt;

    public static BlogCommentDto fromEntity(BlogComment e) {
        BlogCommentDto d = new BlogCommentDto();
        d.id = e.getId();
        d.authorName = e.getUser() != null ? (e.getUser().getUsername() != null ? e.getUser().getUsername() : e.getUser().getEmail()) : "Anonymous";
        d.content = e.getContent();
        d.createdAt = e.getCreatedAt();
        return d;
    }

    public Long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public String getContent() { return content; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

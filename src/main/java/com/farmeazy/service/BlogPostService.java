package com.farmeazy.service;

import com.farmeazy.dto.BlogPostDto;
import com.farmeazy.entity.BlogPost;
import com.farmeazy.entity.BlogPostRating;
import com.farmeazy.entity.Notification;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.BlogPostRepository;
import com.farmeazy.repository.BlogPostRatingRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class BlogPostService {

    private static final Logger logger = LoggerFactory.getLogger(BlogPostService.class);
    private static final String BLOG_SOURCE_ADMIN = "ADMIN_PORTAL";
    private static final String BLOG_SOURCE_USER = "USER_PORTAL";

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogPostRatingRepository blogPostRatingRepository;

    @Autowired
    private com.farmeazy.repository.BlogCommentRepository blogCommentRepository;

    public List<BlogPostDto> getPublicPosts(String category) {
        logger.info("BLOG_SERVICE_GET_PUBLIC_POSTS_START category={}", category);
        List<BlogPost> posts;
        if (category != null && !category.isBlank()) {
            posts = blogPostRepository.findByStatusAndCategoryIgnoreCaseOrderByPublishedAtDesc(BlogPost.BlogStatus.PUBLISHED, category.trim());
        } else {
            posts = blogPostRepository.findByStatusOrderByPublishedAtDesc(BlogPost.BlogStatus.PUBLISHED);
        }
        logger.info("BLOG_SERVICE_GET_PUBLIC_POSTS_DONE count={} category={}", posts.size(), category);
        return posts.stream().map(this::toDto).toList();
    }

    public BlogPostDto getPublicPostBySlug(String slug) {
        logger.info("BLOG_SERVICE_GET_PUBLIC_BY_SLUG_START slug={}", slug);
        BlogPost post = blogPostRepository.findBySlug(slug)
                .filter(p -> p.getStatus() == BlogPost.BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published blog post not found: " + slug));
        logger.info("BLOG_SERVICE_GET_PUBLIC_BY_SLUG_DONE slug={} id={}", slug, post.getId());
        return toDto(post);
    }

    public List<BlogPostDto> getAllPostsForAdmin() {
        logger.info("BLOG_SERVICE_GET_ALL_FOR_ADMIN_START");
        List<BlogPostDto> result = blogPostRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList();
        logger.info("BLOG_SERVICE_GET_ALL_FOR_ADMIN_DONE count={}", result.size());
        return result;
    }

    public List<BlogPostDto> getUserSubmittedPosts(String actorEmail) {
        logger.info("BLOG_SERVICE_GET_USER_SUBMISSIONS actorEmail={}", actorEmail);
        return blogPostRepository.findByCreatedByAndSourceOrderByUpdatedAtDesc(actorEmail, BLOG_SOURCE_USER)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public BlogPostDto getPostForAdmin(Long id) {
        logger.info("BLOG_SERVICE_GET_ADMIN_BY_ID_START id={}", id);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + id));
        logger.info("BLOG_SERVICE_GET_ADMIN_BY_ID_DONE id={} status={}", id, post.getStatus());
        return toDto(post);
    }

    @Transactional
    public BlogPostDto createPost(BlogPostDto dto, String actor) {
        logger.info("BLOG_SERVICE_CREATE_START actor={} title={}", actor, dto != null ? dto.getTitle() : null);
        BlogPost post = new BlogPost();
        applyMutableFields(post, dto);
        post.setSlug(generateUniqueSlug(dto.getTitle(), null));
        post.setStatus(resolveAdminInputStatus(dto.getStatus()));
        post.setSource(BLOG_SOURCE_ADMIN);
        post.setCreatedBy(actor);
        post.setUpdatedBy(actor);
        post.setPublishedAt(null);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_CREATE_DONE actor={} id={} status={}", actor, saved.getId(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto submitUserPost(BlogPostDto dto, String actorEmail, String actorName) {
        logger.info("BLOG_SERVICE_USER_SUBMIT_START actorEmail={} title={}", actorEmail, dto != null ? dto.getTitle() : null);
        BlogPost post = new BlogPost();
        applyMutableFields(post, dto);
        post.setSlug(generateUniqueSlug(dto.getTitle(), null));
        post.setStatus(BlogPost.BlogStatus.PENDING_APPROVAL);
        post.setSource(BLOG_SOURCE_USER);
        post.setCreatedBy(actorEmail);
        post.setUpdatedBy(actorEmail);
        post.setPublishedAt(null);
        if (post.getAuthorName() == null || post.getAuthorName().isBlank()) {
            post.setAuthorName(actorName != null && !actorName.isBlank() ? actorName : actorEmail);
        }

        BlogPost saved = blogPostRepository.save(post);
        notifyAdminsForUserSubmission(saved, actorEmail);

        logger.info("BLOG_SERVICE_USER_SUBMIT_DONE actorEmail={} id={} status={}", actorEmail, saved.getId(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto updatePost(Long id, BlogPostDto dto, String actor) {
        logger.info("BLOG_SERVICE_UPDATE_START id={} actor={}", id, actor);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + id));

        applyMutableFields(post, dto);
        post.setSlug(generateUniqueSlug(dto.getTitle(), post.getId()));
        if (dto.getStatus() != null) {
            BlogPost.BlogStatus resolved = resolveAdminInputStatus(dto.getStatus());
            post.setStatus(resolved);
            if (resolved != BlogPost.BlogStatus.PUBLISHED) {
                post.setPublishedAt(null);
            }
        }
        post.setUpdatedBy(actor);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_UPDATE_DONE id={} actor={} status={}", id, actor, saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto submitForApproval(Long id, String actor) {
        logger.info("BLOG_SERVICE_SUBMIT_APPROVAL_START id={} actor={}", id, actor);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + id));
        post.setStatus(BlogPost.BlogStatus.PENDING_APPROVAL);
        post.setPublishedAt(null);
        post.setUpdatedBy(actor);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_SUBMIT_APPROVAL_DONE id={} actor={}", id, actor);
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto approveAndPublish(Long id, String actor) {
        logger.info("BLOG_SERVICE_APPROVE_AND_PUBLISH_START id={} actor={}", id, actor);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + id));
        post.setStatus(BlogPost.BlogStatus.PUBLISHED);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(OffsetDateTime.now());
        }
        post.setUpdatedBy(actor);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_APPROVE_AND_PUBLISH_DONE id={} actor={} publishedAt={}", id, actor, saved.getPublishedAt());
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto publishPost(Long id, String actor) {
        logger.debug("BLOG_SERVICE_PUBLISH_ALIAS id={} actor={}", id, actor);
        return approveAndPublish(id, actor);
    }

    @Transactional
    public BlogPostDto unpublishPost(Long id, String actor) {
        logger.info("BLOG_SERVICE_UNPUBLISH_START id={} actor={}", id, actor);
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found: " + id));
        post.setStatus(BlogPost.BlogStatus.DRAFT);
        post.setPublishedAt(null);
        post.setUpdatedBy(actor);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_UNPUBLISH_DONE id={} actor={}", id, actor);
        return toDto(saved);
    }

    @Transactional
    public BlogPostDto ratePost(String slug, String actorEmail, int ratingValue) {
        logger.info("BLOG_SERVICE_RATE_START slug={} actorEmail={} rating={}", slug, actorEmail, ratingValue);

        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        BlogPost post = blogPostRepository.findBySlug(slug)
                .filter(item -> item.getStatus() == BlogPost.BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published blog post not found: " + slug));

        User user = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for rating"));

        BlogPostRating rating = blogPostRatingRepository.findByBlogPostIdAndUserId(post.getId(), user.getId())
                .orElseGet(() -> {
                    BlogPostRating created = new BlogPostRating();
                    created.setBlogPost(post);
                    created.setUser(user);
                    return created;
                });

        rating.setRating(ratingValue);
        blogPostRatingRepository.save(rating);

        recalculateAggregates(post);
        BlogPost saved = blogPostRepository.save(post);
        logger.info("BLOG_SERVICE_RATE_DONE slug={} actorEmail={} avg={} count={}", slug, actorEmail, saved.getAverageRating(), saved.getRatingCount());
        return toDto(saved);
    }

    public List<com.farmeazy.dto.BlogCommentDto> getComments(String slug) {
        BlogPost post = blogPostRepository.findBySlug(slug)
                .filter(item -> item.getStatus() == BlogPost.BlogStatus.PUBLISHED)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("Published blog post not found: " + slug));

        List<com.farmeazy.entity.BlogComment> rows = blogCommentRepository.findByBlogPostIdOrderByCreatedAtAsc(post.getId());
        return rows.stream().map(com.farmeazy.dto.BlogCommentDto::fromEntity).toList();
    }

    @Transactional
    public com.farmeazy.dto.BlogCommentDto addComment(String slug, String actorEmail, String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Comment cannot be empty");
        BlogPost post = blogPostRepository.findBySlug(slug)
                .filter(item -> item.getStatus() == BlogPost.BlogStatus.PUBLISHED)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("Published blog post not found: " + slug));

        com.farmeazy.entity.BlogComment comment = new com.farmeazy.entity.BlogComment();
        comment.setBlogPost(post);
        if (actorEmail != null && !actorEmail.isBlank()) {
            com.farmeazy.entity.User user = userRepository.findByEmail(actorEmail).orElse(null);
            comment.setUser(user);
        }
        comment.setContent(content.trim());
        comment = blogCommentRepository.save(comment);

        // Notify post author if possible
        try {
            if (post.getCreatedBy() != null && !post.getCreatedBy().isBlank()) {
                userRepository.findByEmail(post.getCreatedBy()).ifPresent(author -> {
                    String title = "New comment on your blog post";
                    String message = "A new comment was added to '" + post.getTitle() + "'";
                    notificationService.createForUser(author, com.farmeazy.entity.Notification.NotificationType.SYSTEM, title, message, "/blog/" + post.getSlug(), com.farmeazy.entity.Notification.NotificationPriority.NORMAL);
                });
            }
        } catch (Exception ex) {
            // ignore notification failures
        }

        return com.farmeazy.dto.BlogCommentDto.fromEntity(comment);
    }

    private void recalculateAggregates(BlogPost post) {
        List<BlogPostRating> ratings = blogPostRatingRepository.findByBlogPostId(post.getId());
        int count = ratings.size();
        double avg = count == 0
                ? 0.0
                : ratings.stream().mapToInt(BlogPostRating::getRating).average().orElse(0.0);
        post.setRatingCount(count);
        post.setAverageRating(Math.round(avg * 10.0) / 10.0);
    }

    private BlogPost.BlogStatus resolveAdminInputStatus(BlogPost.BlogStatus status) {
        if (status == null) {
            return BlogPost.BlogStatus.DRAFT;
        }
        if (status == BlogPost.BlogStatus.PUBLISHED) {
            // Publishing must go through explicit approval endpoint.
            return BlogPost.BlogStatus.PENDING_APPROVAL;
        }
        return status;
    }

    private void notifyAdminsForUserSubmission(BlogPost post, String actorEmail) {
        String title = "New user blog submitted";
        String message = "User " + actorEmail + " submitted blog '" + post.getTitle() + "' for review.";

        try {
            List<User> adminUsers = userRepository.findAll().stream()
                    .filter(user -> user.getRoles() != null && user.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPERADMIN".equalsIgnoreCase(role)))
                    .toList();

            for (User adminUser : adminUsers) {
                notificationService.createForUser(
                        adminUser,
                        Notification.NotificationType.SYSTEM,
                        title,
                        message,
                        "/admin/blog-posts",
                        Notification.NotificationPriority.HIGH
                );
            }
            logger.info("BLOG_SERVICE_USER_SUBMIT_NOTIFY_ADMINS_DONE count={} postId={}", adminUsers.size(), post.getId());
        } catch (Exception ex) {
            logger.warn("BLOG_SERVICE_USER_SUBMIT_NOTIFY_ADMINS_FAILED postId={} message={}", post.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void deletePost(Long id) {
        logger.info("BLOG_SERVICE_DELETE_START id={}", id);
        if (!blogPostRepository.existsById(id)) {
            logger.error("BLOG_SERVICE_DELETE_NOT_FOUND id={}", id);
            throw new ResourceNotFoundException("Blog post not found: " + id);
        }
        blogPostRepository.deleteById(id);
        logger.info("BLOG_SERVICE_DELETE_DONE id={}", id);
    }

    private void applyMutableFields(BlogPost post, BlogPostDto dto) {
        post.setTitle(dto.getTitle() != null ? dto.getTitle().trim() : null);
        post.setExcerpt(dto.getExcerpt() != null ? dto.getExcerpt().trim() : null);
        post.setContent(dto.getContent() != null ? dto.getContent().trim() : null);
        post.setCategory(dto.getCategory() != null ? dto.getCategory().trim() : null);
        post.setCoverImageUrl(dto.getCoverImageUrl() != null ? dto.getCoverImageUrl().trim() : null);
        post.setAuthorName(dto.getAuthorName() != null ? dto.getAuthorName().trim() : null);
        post.setTags(joinTags(dto.getTags()));
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return tags.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private List<String> splitTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsCsv.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String generateUniqueSlug(String title, Long currentId) {
        String base = slugify(title);
        String candidate = base;
        int suffix = 2;

        while (true) {
            final String checking = candidate;
            BlogPost existing = blogPostRepository.findBySlug(checking).orElse(null);
            if (existing == null || (currentId != null && existing.getId().equals(currentId))) {
                logger.debug("BLOG_SERVICE_SLUG_SELECTED title={} slug={}", title, candidate);
                return candidate;
            }
            candidate = base + "-" + suffix;
            suffix++;
        }
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "blog-post";
        }
        String cleaned = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return cleaned.isBlank() ? "blog-post" : cleaned;
    }

    private BlogPostDto toDto(BlogPost post) {
        BlogPostDto dto = new BlogPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setContent(post.getContent());
        dto.setCategory(post.getCategory());
        dto.setTags(splitTags(post.getTags()));
        dto.setCoverImageUrl(post.getCoverImageUrl());
        dto.setAuthorName(post.getAuthorName());
        dto.setSource(post.getSource());
        dto.setStatus(post.getStatus());
        dto.setRatingCount(post.getRatingCount());
        dto.setAverageRating(post.getAverageRating());
        dto.setPublishedAt(post.getPublishedAt());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
}

package com.farmeazy.service;

import com.farmeazy.dto.ProductCreateDto;
import com.farmeazy.dto.ProductDto;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.Product;
import com.farmeazy.entity.ProductMedia;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.ProductRepository;
import com.farmeazy.repository.ProductMediaRepository;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
            /**
             * Update product with new files (images/videos)
             */
            @Transactional
            public ProductDto updateProductWithFiles(Long id, ProductCreateDto dto, String email, List<MultipartFile> files) {
                Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
                if (!product.getSeller().getEmail().equals(email)) {
                    throw new UnauthorizedException("You are not authorized to update this product");
                }
                product.setProductName(dto.getProductName());
                product.setCategory(dto.getCategory());
                product.setDescription(dto.getDescription());
                product.setPrice(dto.getPrice());
                product.setDiscountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0.0);
                product.setQuantity(dto.getQuantity());
                product.setUnit(dto.getUnit());
                product.setWeight(dto.getWeight());
                product.setSpecifications(dto.getSpecifications());
                product.setWarrantyInfo(dto.getWarrantyInfo());
                product.setContactEmail(dto.getContactEmail());
                product.setContactPhone(dto.getContactPhone());
                product.setVendorId(dto.getVendorId());
                product.setVendorName(dto.getVendorName());
                product.setVendorLocation(dto.getVendorLocation());
                product.setVendorType(dto.getVendorType());
                product.setSellerEmail(product.getSeller().getEmail());
                product.setSellerPhone(product.getSeller().getPhone());
                // Handle new files
                product.getMediaFiles().clear();
                String imageUrl = null;
                String videoUrl = null;
                boolean hasImage = false;
                boolean hasVideo = false;
                if (files != null && !files.isEmpty()) {
                    for (MultipartFile file : files) {
                        String fileName = fileStorageService.store(file);
                        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path("/api/products/media/")
                                .path(fileName)
                                .toUriString();
                        String mediaType = file.getContentType().startsWith("image") ? "IMAGE" : "VIDEO";
                        ProductMedia media = new ProductMedia(fileDownloadUri, mediaType, product);
                        product.getMediaFiles().add(media);
                        if (mediaType.equals("IMAGE")) {
                            hasImage = true;
                            imageUrl = fileDownloadUri;
                        }
                        if (mediaType.equals("VIDEO")) {
                            hasVideo = true;
                            videoUrl = fileDownloadUri;
                        }
                    }
                }
                // If no image file uploaded, ensure a ProductMedia entry for image is null
                // REMOVED: Do not add ProductMedia with null mediaUrl (SQL constraint)
                // If no video file uploaded, ensure a ProductMedia entry for video is null
                // REMOVED: Do not add ProductMedia with null mediaUrl (SQL constraint)
                // Store URLs in product table (do not expect from frontend)
                product.setImageUrls(imageUrl);
                product.setVideoUrls(videoUrl);
                Product updatedProduct = productRepository.save(product);

                try {
                    notificationService.createForUser(
                        updatedProduct.getSeller(),
                        NotificationType.PRODUCT,
                        "Product Media Updated",
                        "Media updated for: " + updatedProduct.getProductName(),
                        "/selling",
                        NotificationPriority.NORMAL
                    );
                } catch (Exception ignored) {
                }

                return convertToDto(updatedProduct);
            }
        /**
         * Reserve product quantity for cart
         */
        @Transactional
        public void reserveProductQuantity(Long productId, int quantity) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            if (product.getQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getProductName());
            }
            product.setQuantity(product.getQuantity() - quantity);
            // Auto-update status if quantity is zero or less
            if (product.getQuantity() <= 0) {
                product.setStatus("OUT_OF_STOCK");
            }
            productRepository.save(product);
        }

        /**
         * Release product quantity from cart
         */
        @Transactional
        public void releaseProductQuantity(Long productId, int quantity) {
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            product.setQuantity(product.getQuantity() + quantity);
            // Auto-update status if quantity is positive and was OUT_OF_STOCK
            if (product.getQuantity() > 0 && "OUT_OF_STOCK".equals(product.getStatus())) {
                product.setStatus("ACTIVE");
            }
            productRepository.save(product);
        }
    
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final HttpEmailService httpEmailService;
    private final FileStorageService fileStorageService;
    private final ProductMediaRepository productMediaRepository;
    private final UserActivityService userActivityService;
    private final CoinService coinService;
    private final NotificationService notificationService;

    @Autowired
    public ProductService(ProductRepository productRepository, UserRepository userRepository, HttpEmailService httpEmailService, FileStorageService fileStorageService, ProductMediaRepository productMediaRepository, UserActivityService userActivityService, CoinService coinService, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.httpEmailService = httpEmailService;
        this.fileStorageService = fileStorageService;
        this.productMediaRepository = productMediaRepository;
        this.userActivityService = userActivityService;
        this.coinService = coinService;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public ProductDto createProduct(ProductCreateDto dto, String email, List<MultipartFile> files) {
        User seller = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        Product product = new Product();
        product.setSeller(seller);
        product.setProductName(dto.getProductName());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0.0);
        product.setQuantity(dto.getQuantity());
        product.setUnit(dto.getUnit());
        product.setWeight(dto.getWeight());
        product.setSpecifications(dto.getSpecifications());
        product.setWarrantyInfo(dto.getWarrantyInfo());
        Integer deliveryMin = dto.getDeliveryDaysMin() != null ? dto.getDeliveryDaysMin() : 3;
        Integer deliveryMax = dto.getDeliveryDaysMax() != null ? dto.getDeliveryDaysMax() : 5;
        if (deliveryMin > deliveryMax) {
            deliveryMin = 3;
            deliveryMax = 5;
        }
        product.setDeliveryDaysMin(deliveryMin);
        product.setDeliveryDaysMax(deliveryMax);
        product.setStatus("ACTIVE");
        product.setContactEmail(dto.getContactEmail());
        product.setContactPhone(dto.getContactPhone());
        // Vendor Transparency
        product.setVendorId(dto.getVendorId());
        product.setVendorName(dto.getVendorName());
        product.setVendorLocation(dto.getVendorLocation());
        product.setVendorType(dto.getVendorType());
        // Seller info from User
        product.setSellerEmail(seller.getEmail());
        product.setSellerPhone(seller.getPhone());
        
        String imageUrl = null;
        String videoUrl = null;
        boolean hasImage = false;
        boolean hasVideo = false;
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                String fileName = fileStorageService.store(file);
                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/products/media/")
                        .path(fileName)
                        .toUriString();

                String mediaType = file.getContentType().startsWith("image") ? "IMAGE" : "VIDEO";
                ProductMedia media = new ProductMedia(fileDownloadUri, mediaType, product);
                product.getMediaFiles().add(media);
                if (mediaType.equals("IMAGE")) {
                    hasImage = true;
                    imageUrl = fileDownloadUri;
                }
                if (mediaType.equals("VIDEO")) {
                    hasVideo = true;
                    videoUrl = fileDownloadUri;
                }
            }
        }
        // If no image file uploaded, ensure a ProductMedia entry for image is null
        // REMOVED: Do not add ProductMedia with null mediaUrl (SQL constraint)
        // If no video file uploaded, ensure a ProductMedia entry for video is null
        // REMOVED: Do not add ProductMedia with null mediaUrl (SQL constraint)
        // Store URLs in product table (do not expect from frontend)
        product.setImageUrls(imageUrl);
        product.setVideoUrls(videoUrl);
        
        Product savedProduct = productRepository.save(product);

        try {
            notificationService.createForUser(
                seller,
                NotificationType.PRODUCT,
                "Product Listed Successfully",
                "Your product has been listed: " + savedProduct.getProductName(),
                "/selling",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Log activity
        userActivityService.logActivity(seller, com.farmeazy.entity.UserActivity.ActivityType.PRODUCT_LISTED, "Listed a new product: " + savedProduct.getProductName());

        // Award coins for listing a product (15 coins)
        try {
            coinService.addCoins(seller.getEmail(), 15, "Listed product: " + savedProduct.getProductName());
        } catch (Exception e) {
            System.err.println("Failed to award coins for product listing: " + e.getMessage());
        }

        // Send confirmation email using HTTP service (works on Render)
        try {
            httpEmailService.sendProductListingConfirmation(
                seller.getEmail(),
                seller.getUsername(),
                savedProduct.getProductName(),
                savedProduct.getCategory(),
                savedProduct.getPrice(),
                savedProduct.getQuantity(),
                savedProduct.getUnit()
            );
        } catch (Exception e) {
            // Log error but don't fail product creation
            System.err.println("Failed to send product listing email: " + e.getMessage());
        }
        
        return convertToDto(savedProduct);
    }
    
    public List<ProductDto> getAllActiveProducts() {
        return productRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "ACTIVE").stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public List<ProductDto> getMyProducts(String email) {
        User seller = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return productRepository.findBySellerOrderByCreatedAtDesc(seller).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public int getMyProductsCount(String email) {
        User seller = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return productRepository.findBySeller(seller).size();
    }
    
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findWithDetailsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return convertToDto(product);
    }
    
    @Transactional
    public ProductDto updateProduct(Long id, ProductCreateDto dto, String email) {
        // TODO: Handle file updates
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        if (!product.getSeller().getEmail().equals(email)) {
            throw new UnauthorizedException("You are not authorized to update this product");
        }
        
        product.setProductName(dto.getProductName());
        product.setCategory(dto.getCategory());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0.0);
        product.setQuantity(dto.getQuantity());
        product.setUnit(dto.getUnit());
        product.setWeight(dto.getWeight());
        product.setSpecifications(dto.getSpecifications());
        product.setWarrantyInfo(dto.getWarrantyInfo());
        Integer deliveryMin = dto.getDeliveryDaysMin() != null ? dto.getDeliveryDaysMin() : 3;
        Integer deliveryMax = dto.getDeliveryDaysMax() != null ? dto.getDeliveryDaysMax() : 5;
        if (deliveryMin > deliveryMax) {
            deliveryMin = 3;
            deliveryMax = 5;
        }
        product.setDeliveryDaysMin(deliveryMin);
        product.setDeliveryDaysMax(deliveryMax);
        product.setContactEmail(dto.getContactEmail());
        product.setContactPhone(dto.getContactPhone());
        // Vendor Transparency
        product.setVendorId(dto.getVendorId());
        product.setVendorName(dto.getVendorName());
        product.setVendorLocation(dto.getVendorLocation());
        product.setVendorType(dto.getVendorType());
        // Seller info from User
        product.setSellerEmail(product.getSeller().getEmail());
        product.setSellerPhone(product.getSeller().getPhone());
        
        Product updatedProduct = productRepository.save(product);

        try {
            notificationService.createForUser(
                updatedProduct.getSeller(),
                NotificationType.PRODUCT,
                "Product Updated",
                "Your product has been updated: " + updatedProduct.getProductName(),
                "/selling",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }
        
        // Log activity
        userActivityService.logActivity(updatedProduct.getSeller(), com.farmeazy.entity.UserActivity.ActivityType.PRODUCT_UPDATED, "Updated product: " + updatedProduct.getProductName());

        // Send update email notification
        try {
            httpEmailService.sendProductUpdateConfirmation(
                updatedProduct.getSeller().getEmail(),
                updatedProduct.getSeller().getUsername(),
                updatedProduct.getProductName(),
                updatedProduct.getCategory(),
                updatedProduct.getPrice(),
                updatedProduct.getQuantity(),
                updatedProduct.getUnit()
            );
        } catch (Exception e) {
            System.err.println("Failed to send product update email: " + e.getMessage());
        }
        
        return convertToDto(updatedProduct);
    }
    
    @Transactional
    public void deleteProduct(Long id, String email) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        if (!product.getSeller().getEmail().equals(email)) {
            throw new UnauthorizedException("You are not authorized to delete this product");
        }
        
        // Log activity before deleting
        userActivityService.logActivity(product.getSeller(), com.farmeazy.entity.UserActivity.ActivityType.PRODUCT_DELETED, "Deleted product: " + product.getProductName());

        try {
            notificationService.createForUser(
                product.getSeller(),
                NotificationType.PRODUCT,
                "Product Removed",
                "Your product has been removed: " + product.getProductName(),
                "/selling",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Send delete email notification
        try {
            httpEmailService.sendProductDeleteConfirmation(
                product.getSeller().getEmail(),
                product.getSeller().getUsername(),
                product.getProductName(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity(),
                product.getUnit()
            );
        } catch (Exception e) {
            System.err.println("Failed to send product delete email: " + e.getMessage());
        }
        
        productRepository.delete(product);
    }
    
    @Transactional
    public ProductDto updateProductStatus(Long id, String status, String email) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        if (!product.getSeller().getEmail().equals(email)) {
            throw new UnauthorizedException("You are not authorized to update this product");
        }
        
        product.setStatus(status);
        Product updatedProduct = productRepository.save(product);
        
        // Log activity
        String activityMessage = String.format("Updated product status to %s for: %s", status, updatedProduct.getProductName());
        userActivityService.logActivity(updatedProduct.getSeller(), com.farmeazy.entity.UserActivity.ActivityType.PRODUCT_STATUS_CHANGED, activityMessage);

        return convertToDto(updatedProduct);
    }
    
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setSellerId(product.getSeller().getId());
        dto.setSellerUsername(product.getSeller().getUsername());
        dto.setSellerFullName(product.getSeller().getUsername());
        dto.setSellerEmail(product.getSeller().getEmail());
        dto.setSellerPhone(product.getSeller().getPhone());
        dto.setSellerLocation(product.getSeller().getCity() != null ? product.getSeller().getCity() : "Location not specified");
        dto.setProductName(product.getProductName());
        dto.setCategory(product.getCategory());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setDiscountPercentage(product.getDiscountPercentage());
        dto.setDiscountedPrice(product.getDiscountedPrice());
        dto.setQuantity(product.getQuantity());
        dto.setUnit(product.getUnit());
        dto.setWeight(product.getWeight());
        dto.setSpecifications(product.getSpecifications());
        dto.setWarrantyInfo(product.getWarrantyInfo());
        dto.setDeliveryDaysMin(product.getDeliveryDaysMin());
        dto.setDeliveryDaysMax(product.getDeliveryDaysMax());
        dto.setStatus(product.getStatus());
        dto.setContactEmail(product.getContactEmail());
        dto.setContactPhone(product.getContactPhone());
        // Vendor Transparency
        dto.setVendorId(product.getVendorId());
        dto.setVendorName(product.getVendorName());
        dto.setVendorLocation(product.getVendorLocation());
        dto.setVendorType(product.getVendorType());
        
        // Set mediaUrls for gallery
        if (product.getMediaFiles() != null) {
            dto.setMediaUrls(product.getMediaFiles().stream()
                .map(ProductMedia::getMediaUrl)
                .filter(url -> url != null && !url.isEmpty())
                .collect(Collectors.toList()));
        }
        // Set imageUrls as comma-separated string (for frontend compatibility)
        if (product.getMediaFiles() != null) {
            String imageUrls = product.getMediaFiles().stream()
                .filter(media -> "IMAGE".equals(media.getMediaType()) && media.getMediaUrl() != null && !media.getMediaUrl().isEmpty())
                .map(ProductMedia::getMediaUrl)
                .collect(Collectors.joining(","));
            dto.setImageUrls(imageUrls);
            String videoUrls = product.getMediaFiles().stream()
                .filter(media -> "VIDEO".equals(media.getMediaType()) && media.getMediaUrl() != null && !media.getMediaUrl().isEmpty())
                .map(ProductMedia::getMediaUrl)
                .collect(Collectors.joining(","));
            dto.setVideoUrls(videoUrls);
        }

        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.ProductCreateDto;
import com.farmeazy.dto.ProductDto;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.service.FileStorageService;
import com.farmeazy.service.ListingEligibilityService;
import com.farmeazy.service.ProductService;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserBankDetailsRepository;
import com.farmeazy.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class ProductController {
        /**
         * Reserve product quantity for cart
         */
        @PostMapping("/{id}/reserve")
        public ResponseEntity<Map<String, String>> reserveProduct(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
            int quantity = body.getOrDefault("quantity", 1);
            productService.reserveProductQuantity(id, quantity);
            return ResponseEntity.ok(Map.of("message", "Product quantity reserved"));
        }

        /**
         * Release product quantity from cart
         */
        @PostMapping("/{id}/release")
        public ResponseEntity<Map<String, String>> releaseProduct(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
            int quantity = body.getOrDefault("quantity", 1);
            productService.releaseProductQuantity(id, quantity);
            return ResponseEntity.ok(Map.of("message", "Product quantity released"));
        }
    
    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final ListingEligibilityService listingEligibilityService;
    private final UserBankDetailsRepository userBankDetailsRepository;
    
    @Autowired
    public ProductController(ProductService productService, FileStorageService fileStorageService, UserService userService, ListingEligibilityService listingEligibilityService, UserBankDetailsRepository userBankDetailsRepository) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
        this.listingEligibilityService = listingEligibilityService;
        this.userBankDetailsRepository = userBankDetailsRepository;
    }
    
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductDto> createProduct(@RequestParam("product") String productStr, @RequestParam(value = "files", required = false) List<MultipartFile> files, Authentication authentication) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductCreateDto dto = objectMapper.readValue(productStr, ProductCreateDto.class);
        
        String email = authentication.getName();
        User seller = userService.findByEmail(email);
        listingEligibilityService.assertEligible(seller, "PRODUCT");

        UserBankDetails bankDetails = userBankDetailsRepository.findByUserId(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor details not found. Please complete bank details first."));

        // Always derive vendor profile server-side to prevent payload-based onboarding.
        dto.setVendorId(seller.getId());
        dto.setVendorName(bankDetails.getAccountHolderName());
        dto.setVendorLocation(formatLocation(seller));
        dto.setVendorType("VERIFIED_VENDOR");

        ProductDto createdProduct = productService.createProduct(dto, email, files);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    private String formatLocation(User seller) {
        String city = seller.getCity() != null ? seller.getCity().trim() : "";
        String state = seller.getState() != null ? seller.getState().trim() : "";
        if (!city.isEmpty() && !state.isEmpty()) {
            return city + ", " + state;
        }
        return !city.isEmpty() ? city : state;
    }
    
    @GetMapping("/media/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = fileStorageService.loadAsResource(filename);
        String contentType = detectContentType(filename);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
            .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
            .body(file);
    }

    private String detectContentType(String filename) {
        String safeName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (safeName.endsWith(".png")) return "image/png";
        if (safeName.endsWith(".jpg") || safeName.endsWith(".jpeg")) return "image/jpeg";
        if (safeName.endsWith(".webp")) return "image/webp";
        if (safeName.endsWith(".gif")) return "image/gif";
        if (safeName.endsWith(".mp4")) return "video/mp4";
        if (safeName.endsWith(".webm")) return "video/webm";
        if (safeName.endsWith(".ogg")) return "video/ogg";
        if (safeName.endsWith(".m4v")) return "video/x-m4v";
        if (safeName.endsWith(".mov")) return "video/quicktime";

        try {
            Path filePath = fileStorageService.load(filename);
            String probed = Files.probeContentType(filePath);
            if (probed != null && !probed.isBlank()) {
                return probed;
            }
        } catch (Exception ignored) {
        }

        return "application/octet-stream";
    }
    
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllActiveProducts();
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(@PathVariable String category) {
        List<ProductDto> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/my-products")
    public ResponseEntity<List<ProductDto>> getMyProducts(Authentication authentication) {
        String email = authentication.getName();
        List<ProductDto> products = productService.getMyProducts(email);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/my-products/count")
    public ResponseEntity<Map<String, Integer>> getMyProductsCount(Authentication authentication) {
        String email = authentication.getName();
        int count = productService.getMyProductsCount(email);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestParam("product") String productStr, @RequestParam(value = "files", required = false) List<MultipartFile> files, Authentication authentication) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductCreateDto dto = objectMapper.readValue(productStr, ProductCreateDto.class);
        String email = authentication.getName();
        ProductDto updatedProduct = productService.updateProductWithFiles(id, dto, email, files);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        productService.deleteProduct(id, email);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductDto> updateProductStatus(@PathVariable Long id, @RequestBody Map<String, String> statusMap, Authentication authentication) {
        String email = authentication.getName();
        String status = statusMap.get("status");
        ProductDto updatedProduct = productService.updateProductStatus(id, status, email);
        return ResponseEntity.ok(updatedProduct);
    }
}

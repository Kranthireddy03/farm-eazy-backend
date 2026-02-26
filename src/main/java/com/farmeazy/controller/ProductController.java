package com.farmeazy.controller;

import com.farmeazy.dto.ProductCreateDto;
import com.farmeazy.dto.ProductDto;
import com.farmeazy.service.FileStorageService;
import com.farmeazy.service.ProductService;
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
import java.util.List;
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
    
    @Autowired
    public ProductController(ProductService productService, FileStorageService fileStorageService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
    }
    
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ProductDto> createProduct(@RequestParam("product") String productStr, @RequestParam(value = "files", required = false) List<MultipartFile> files, Authentication authentication) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ProductCreateDto dto = objectMapper.readValue(productStr, ProductCreateDto.class);
        
        String email = authentication.getName();
        ProductDto createdProduct = productService.createProduct(dto, email, files);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    
    @GetMapping("/media/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = fileStorageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + file.getFilename() + "\"").body(file);
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
        // Note: The update logic in service needs to handle file updates.
        ProductDto updatedProduct = productService.updateProduct(id, dto, email);
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

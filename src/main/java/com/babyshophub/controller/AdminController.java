package com.babyshophub.controller;

import com.babyshophub.entity.Product;
import com.babyshophub.dto.*;
import com.babyshophub.service.AdminService;
import com.babyshophub.service.CategoryService;
import com.babyshophub.service.ProductService;
import com.babyshophub.service.OrderService;
import com.babyshophub.service.SupportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Secures all endpoints in this controller to Admins only
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final AdminService adminService;
    private final OrderService orderService;
    private final SupportService supportService;

    public AdminController(ProductService productService, CategoryService categoryService, AdminService adminService, OrderService orderService, SupportService supportService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.adminService = adminService;
        this.orderService = orderService;
        this.supportService = supportService;
    }

    // Admin: Create product with optional image upload
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestPart("product") ProductCreateRequest request,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("brandId") Long brandId,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setImageUrls(new java.util.ArrayList<>(request.getImageUrls()));
        if (imageFile != null && !imageFile.isEmpty()) {
            product.getImageUrls().add(productService.saveImageLocally(imageFile));
        }
        Product savedProduct = productService.createProduct(product, categoryId, brandId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(savedProduct));
    }

    // Admin: Add a new category
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        var category = new com.babyshophub.entity.Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(categoryService.createCategory(category)));
    }

    // Admin: Get all users
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    // Admin: View all recorded user activity (login, registration, status changes, etc.)
    @GetMapping("/activity")
    public ResponseEntity<List<ActivityResponse>> getActivity() {
        return ResponseEntity.ok(adminService.allActivity());
    }

    // Admin: View activity for a single user
    @GetMapping("/users/{id}/activity")
    public ResponseEntity<List<ActivityResponse>> getUserActivity(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.activity(id));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getProducts() { return ResponseEntity.ok(productService.getAllProducts().stream().map(ProductResponse::from).toList()); }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productService.updateProduct(id, request)));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ProductResponse> patchProduct(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ProductResponse.from(productService.updateProduct(id, request)));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id); return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserSummaryResponse> updateUserStatus(@PathVariable Long id, @RequestBody AccountStatusRequest request) {
        return ResponseEntity.ok(adminService.setSuspended(id, request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders() { return ResponseEntity.ok(orderService.allOrders()); }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusRequest request) { return ResponseEntity.ok(orderService.changeOrderStatus(id, request.getStatus())); }

    @GetMapping("/support")
    public ResponseEntity<List<SupportResponse>> getSupportTickets() { return ResponseEntity.ok(supportService.all()); }

    @PatchMapping("/support/{id}/status")
    public ResponseEntity<SupportResponse> updateSupportStatus(@PathVariable Long id, @Valid @RequestBody SupportStatusRequest request) { return ResponseEntity.ok(supportService.status(id, request)); }
}

package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.CategoryDTO;
import com.cc91.dto.CreateCategoryRequest;
import com.cc91.dto.UpdateCategoryRequest;
import com.cc91.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 - 版块管理控制器
 */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 创建版块
     * POST /api/admin/categories
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryDTO category = categoryService.create(request);
        return ResponseEntity.ok(ApiResponse.success("版块创建成功", category));
    }

    /**
     * 更新版块
     * PUT /api/admin/categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryDTO category = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("版块更新成功", category));
    }

    /**
     * 删除版块
     * DELETE /api/admin/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("版块删除成功"));
    }
}

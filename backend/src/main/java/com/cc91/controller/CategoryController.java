package com.cc91.controller;

import com.cc91.dto.ApiResponse;
import com.cc91.dto.CategoryDTO;
import com.cc91.dto.CreateCategoryRequest;
import com.cc91.dto.UpdateCategoryRequest;
import com.cc91.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版块分类控制器
 * 处理版块分类相关的请求
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取所有版块（按排序顺序）
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * 根据ID获取版块详情
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        CategoryDTO category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * 创建版块
     * POST /api/categories
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
     * PUT /api/categories/{id}
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
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("版块删除成功"));
    }
}

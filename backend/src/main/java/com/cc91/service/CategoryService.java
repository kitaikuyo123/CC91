package com.cc91.service;

import com.cc91.dto.CategoryDTO;
import com.cc91.dto.CreateCategoryRequest;
import com.cc91.dto.UpdateCategoryRequest;
import com.cc91.entity.Category;
import com.cc91.exception.ResourceNotFoundException;
import com.cc91.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 版块分类服务
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * 获取所有版块（按排序顺序）
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toCategoryDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取版块
     */
    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));
        return toCategoryDTO(category);
    }

    /**
     * 创建版块
     */
    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
        // 检查名称是否重复
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("版块名称已存在");
        }

        Category category = new Category(
                request.getName(),
                request.getDescription(),
                request.getSortOrder() != null ? request.getSortOrder() : 0
        );
        category = categoryRepository.save(category);

        logger.info("版块创建成功: id={}, name={}", category.getId(), category.getName());

        return toCategoryDTO(category);
    }

    /**
     * 更新版块
     */
    @Transactional
    public CategoryDTO update(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));

        // 如果修改名称，检查是否重复
        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.findByName(request.getName()).isPresent()) {
                throw new IllegalArgumentException("版块名称已存在");
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        category = categoryRepository.save(category);

        logger.info("版块更新成功: id={}, name={}", category.getId(), category.getName());

        return toCategoryDTO(category);
    }

    /**
     * 删除版块
     */
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));

        categoryRepository.delete(category);

        logger.info("版块删除成功: id={}, name={}", id, category.getName());
    }

    /**
     * 转换为 CategoryDTO
     */
    private CategoryDTO toCategoryDTO(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.getCreatedAt()
        );
    }
}

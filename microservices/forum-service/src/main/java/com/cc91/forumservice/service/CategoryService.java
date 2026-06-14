package com.cc91.forumservice.service;

import com.cc91.forumservice.dto.CategoryDTO;
import com.cc91.forumservice.dto.CreateCategoryRequest;
import com.cc91.forumservice.dto.UpdateCategoryRequest;
import com.cc91.forumservice.entity.Category;
import com.cc91.forumservice.exception.ResourceNotFoundException;
import com.cc91.forumservice.repository.CategoryRepository;
import com.cc91.forumservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 版块分类服务
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryService(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    /**
     * 获取所有版块（按排序顺序）
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAll() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        LocalDateTime todayStart = todayStart();

        Map<Long, Object[]> statsMap = postRepository.countStatsByCategory("PUBLISHED", todayStart)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> row
                ));

        return categories.stream()
                .map(cat -> buildDTO(cat, statsMap.get(cat.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取版块
     */
    @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));
        LocalDateTime todayStart = todayStart();
        long postCount = postRepository.countByCategoryIdAndStatus(id, "PUBLISHED");
        long todayPostCount = postRepository.countByCategoryIdAndStatusAndCreatedAtAfter(id, "PUBLISHED", todayStart);
        return buildDTO(category, postCount, todayPostCount);
    }

    /**
     * 创建版块
     */
    @Transactional
    public CategoryDTO create(CreateCategoryRequest request) {
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

        return buildDTO(category, 0, 0);
    }

    /**
     * 更新版块
     */
    @Transactional
    public CategoryDTO update(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));

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

        return buildDTO(category, 0, 0);
    }

    /**
     * 删除版块
     */
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("版块不存在"));

        // 检查是否有帖子引用此分类
        long postCount = postRepository.countByCategoryId(id);
        if (postCount > 0) {
            throw new IllegalStateException("该版块下还有 " + postCount + " 篇帖子，无法删除");
        }

        categoryRepository.delete(category);

        logger.info("版块删除成功: id={}, name={}", id, category.getName());
    }

    private LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    private CategoryDTO buildDTO(Category cat, long postCount, long todayPostCount) {
        return new CategoryDTO(cat.getId(), cat.getName(), cat.getDescription(),
                cat.getSortOrder(), cat.getCreatedAt(), postCount, todayPostCount);
    }

    private CategoryDTO buildDTO(Category cat, Object[] stats) {
        long postCount = stats != null ? ((Number) stats[1]).longValue() : 0;
        long todayPostCount = stats != null ? ((Number) stats[2]).longValue() : 0;
        return buildDTO(cat, postCount, todayPostCount);
    }
}

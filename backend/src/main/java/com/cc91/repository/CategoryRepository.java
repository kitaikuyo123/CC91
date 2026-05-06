package com.cc91.repository;

import com.cc91.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 版块分类数据访问层
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 按排序顺序查询所有版块
     */
    List<Category> findAllByOrderBySortOrderAsc();

    /**
     * 按名称查询版块
     */
    Optional<Category> findByName(String name);
}

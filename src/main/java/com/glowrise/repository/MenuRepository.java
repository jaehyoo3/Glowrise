package com.glowrise.repository;

import com.glowrise.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByBlogId(Long blogId);
    boolean existsByBlogIdAndUrl(Long blogId, String url);
    List<Menu> findByParentId(Long parentId);
}



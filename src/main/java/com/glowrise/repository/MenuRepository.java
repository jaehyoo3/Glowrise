package com.glowrise.repository;

import com.glowrise.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentId(Long parentId);

    @Query("SELECT COALESCE(MAX(m.orderIndex), -1) FROM Menu m WHERE m.blog.id = :blogId")
    Optional<Integer> findMaxOrderIndexByBlogId(@Param("blogId") Long blogId);

    @Query("SELECT m.id FROM Menu m WHERE m.id = :parentId OR m.parent.id = :parentId")
    List<Long> findAllMenuIdsByParent(@Param("parentId") Long parentId);

    List<Menu> findByBlogIdOrderByOrderIndexAsc(Long blogId);
}



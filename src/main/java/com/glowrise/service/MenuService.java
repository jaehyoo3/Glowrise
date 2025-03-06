package com.glowrise.service;


import com.glowrise.domain.Blog;
import com.glowrise.domain.Menu;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.service.dto.MenuDTO;
import com.glowrise.service.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final BlogRepository blogRepository;

    // 메뉴 생성
    public MenuDTO createMenu(MenuDTO dto) {
        Blog blog = blogRepository.findById(dto.getBlogId())
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + dto.getBlogId()));

        if (menuRepository.existsByBlogIdAndUrl(dto.getBlogId(), dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }

        Menu parent = null;
        if (dto.getParentId() != null) {
            parent = menuRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 메뉴를 찾을 수 없습니다: " + dto.getParentId()));
            if (!parent.getBlog().getId().equals(dto.getBlogId())) {
                throw new IllegalStateException("부모 메뉴는 같은 블로그에 속해야 합니다.");
            }
        }

        Menu menu = menuMapper.toEntity(dto); // MapStruct로 매핑
        menu.setBlog(blog);
        menu.setParent(parent);

        Menu savedMenu = menuRepository.save(menu);
        return menuMapper.toDto(savedMenu); // MapStruct로 DTO 변환
    }

    public MenuDTO updateMenu(Long menuId, MenuDTO dto) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (!menu.getBlog().getId().equals(dto.getBlogId())) {
            throw new IllegalStateException("블로그 ID는 변경할 수 없습니다.");
        }

        if (dto.getUrl() != null && !dto.getUrl().equals(menu.getUrl()) &&
                menuRepository.existsByBlogIdAndUrl(dto.getBlogId(), dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }

        if (dto.getParentId() != null && (menu.getParent() == null || !dto.getParentId().equals(menu.getParent().getId()))) {
            Menu parent = menuRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 메뉴를 찾을 수 없습니다: " + dto.getParentId()));
            if (!parent.getBlog().getId().equals(dto.getBlogId())) {
                throw new IllegalStateException("부모 메뉴는 같은 블로그에 속해야 합니다.");
            }
            menu.setParent(parent);
        } else if (dto.getParentId() == null && menu.getParent() != null) {
            menu.setParent(null);
        }

        menuMapper.partialUpdate(menu, dto); // MapStruct로 부분 업데이트
        Menu updatedMenu = menuRepository.save(menu);
        return menuMapper.toDto(updatedMenu);
    }

    // 메뉴 제거
    public void deleteMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));
        menuRepository.delete(menu);
        // CascadeType.ALL과 orphanRemoval=true로 인해 하위 메뉴와 게시글도 자동 삭제
    }

    // 전체 메뉴 목록 조회
    public List<MenuDTO> getAllMenus() {
        List<Menu> menus = menuRepository.findAll();
        return menuMapper.toDto(menus);
    }

    // 블로그별 메뉴 목록 조회
    public List<MenuDTO> getMenusByBlogId(Long blogId) {
        List<Menu> menus = menuRepository.findByBlogId(blogId);
        return menuMapper.toDto(menus);
    }

    // 특정 메뉴의 하위 메뉴 조회
    public List<MenuDTO> getSubMenus(Long menuId) {
        List<Menu> subMenus = menuRepository.findByParentId(menuId);
        return menuMapper.toDto(subMenus);
    }

    // URL 중복 확인 (블로그 내에서)
    public boolean isUrlAvailable(Long blogId, String url) {
        return !menuRepository.existsByBlogIdAndUrl(blogId, url);
    }
}

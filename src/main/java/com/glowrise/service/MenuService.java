package com.glowrise.service;


import com.glowrise.domain.Blog;
import com.glowrise.domain.Menu;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.MenuDTO;
import com.glowrise.service.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

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
    }

    // 전체 메뉴 목록 조회
    public List<MenuDTO> getAllMenus() {
        List<Menu> menus = menuRepository.findAll();
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

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        // JWTFilter에서 설정한 details에서 userId 추출
        Object details = authentication.getDetails();
        if (details instanceof Map) {
            Object userId = ((Map<?, ?>) details).get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
        }
        // 대안: DB에서 조회
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }

    // 메뉴 생성
    public MenuDTO createMenu(MenuDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(dto.getBlogId())
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + dto.getBlogId()));

        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 수정할 권한이 없습니다.");
        }

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

        Menu menu = menuMapper.toEntity(dto);
        menu.setBlog(blog);
        menu.setParent(parent);

        if (menu.getOrderIndex() == null) {
            Integer maxOrder = menuRepository.findMaxOrderIndexByBlogId(dto.getBlogId()).orElse(-1);
            menu.setOrderIndex(maxOrder + 1);
        }
        Menu savedMenu = menuRepository.save(menu);
        MenuDTO savedDto = menuMapper.toDto(savedMenu);

        // 부모 메뉴일 경우 subMenuIds 초기화
        if (dto.getParentId() == null) {
            savedDto.setSubMenuIds(new ArrayList<>());
        }
        return savedDto;
    }

    // 블로그별 메뉴 목록 조회 (권한 체크 추가)
    public List<MenuDTO> getMenusByBlogId(Long blogId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 조회할 권한이 없습니다.");
        }
        List<Menu> menus = menuRepository.findByBlogId(blogId);
        return menuMapper.toDto(menus);
    }

    // 순서 업데이트 (드래그 앤 드롭 반영)
    @Transactional
    public void updateMenuOrder(Long blogId, List<MenuDTO> menus) {
        for (MenuDTO dto : menus) {
            Menu menu = menuRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getId()));
            menu.setOrderIndex(dto.getOrderIndex());
            // parentId 반영
            if (dto.getParentId() != null) {
                Menu parent = menuRepository.findById(dto.getParentId())
                        .orElseThrow(() -> new IllegalArgumentException("부모 메뉴를 찾을 수 없습니다: " + dto.getParentId()));
                menu.setParent(parent);
            } else {
                menu.setParent(null); // 최상위 메뉴로 설정
            }
            menuRepository.save(menu);
        }
    }
}

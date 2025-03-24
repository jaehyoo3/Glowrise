package com.glowrise.service;

import com.glowrise.domain.*;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.MenuDTO;
import com.glowrise.service.mapper.MenuMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    public MenuDTO createMenu(MenuDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(dto.getBlogId())
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + dto.getBlogId()));

        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그의 메뉴를 생성할 권한이 없습니다.");
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
        return menuMapper.toDto(savedMenu);
    }

    public List<MenuDTO> getMenusByBlogId(Long blogId) {
        List<Menu> menus = menuRepository.findByBlogId(blogId);
        menus.sort(Comparator.comparing(Menu::getOrderIndex));
        return menus.stream().map(menu -> {
            MenuDTO dto = new MenuDTO();
            dto.setId(menu.getId());
            dto.setName(menu.getName());
            dto.setOrderIndex(menu.getOrderIndex());
            dto.setBlogId(menu.getBlog().getId());
            dto.setParentId(menu.getParent() != null ? menu.getParent().getId() : null);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateMenuOrder(Long blogId, List<MenuDTO> menus, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));
        if (!blog.getUser().getId().equals(userId)) {
            log.warn("User {} does not have permission to update menu order for blogId: {}", userId, blogId);
            throw new IllegalStateException("해당 블로그의 메뉴를 수정할 권한이 없습니다.");
        }

        for (MenuDTO dto : menus) {
            Menu menu = menuRepository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + dto.getId()));

            if (!menu.getBlog().getId().equals(blogId)) {
                throw new IllegalArgumentException("메뉴가 해당 블로그에 속하지 않습니다: " + dto.getId());
            }

            menu.setOrderIndex(dto.getOrderIndex());
            if (dto.getParentId() != null) {
                Menu parent = menuRepository.findById(dto.getParentId())
                        .orElseThrow(() -> new IllegalArgumentException("부모 메뉴를 찾을 수 없습니다: " + dto.getParentId()));
                if (!parent.getBlog().getId().equals(blogId)) {
                    throw new IllegalArgumentException("부모 메뉴가 같은 블로그에 속해야 합니다: " + dto.getParentId());
                }
                menu.setParent(parent);
            } else {
                menu.setParent(null);
            }
            menuRepository.save(menu);
        }
    }

    public MenuDTO updateMenu(Long menuId, MenuDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));

        if (!menu.getBlog().getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그의 메뉴를 수정할 권한이 없습니다.");
        }

        if (!menu.getBlog().getId().equals(dto.getBlogId())) {
            throw new IllegalStateException("블로그 ID는 변경할 수 없습니다.");
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

        menu.setName(dto.getName());
        menu.setOrderIndex(dto.getOrderIndex());
        Menu updatedMenu = menuRepository.save(menu);
        return menuMapper.toDto(updatedMenu);
    }

    public void deleteMenu(Long menuId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다: " + menuId));
        if (!menu.getBlog().getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그의 메뉴를 삭제할 권한이 없습니다.");
        }
        menuRepository.delete(menu);
    }

    public List<MenuDTO> getAllMenus() {
        List<Menu> menus = menuRepository.findAll();
        return menuMapper.toDto(menus);
    }

    public List<MenuDTO> getSubMenus(Long menuId) {
        List<Menu> subMenus = menuRepository.findByParentId(menuId);
        return menuMapper.toDto(subMenus);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }
}
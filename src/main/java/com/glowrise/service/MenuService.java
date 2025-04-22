package com.glowrise.service;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Menu;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.service.dto.MenuDTO;
import com.glowrise.service.mapper.MenuMapper;
import com.glowrise.service.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final BlogRepository blogRepository;
    private final SecurityUtil securityUtil; // Added

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwner(#dto.blogId)")
    public MenuDTO createMenu(MenuDTO dto, Authentication ignoredAuthentication) {
        Blog blog = findBlogByIdOrThrow(dto.getBlogId());

        Menu menu = menuMapper.toEntity(dto);

        menu.setBlog(blog);

        if (dto.getParentId() != null) {
            Menu parent = findMenuByIdOrThrow(dto.getParentId());
            validateParentMenu(parent, blog.getId());
            menu.setParent(parent);
        } else {
            menu.setParent(null);
        }

        if (menu.getOrderIndex() == null) {
            menu.setOrderIndex(calculateNextOrderIndex(blog.getId()));
        }

        Menu savedMenu = menuRepository.save(menu);
        return menuMapper.toDto(savedMenu);
    }

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwner(#blogId)")
    public void updateMenuOrder(Long blogId, List<MenuDTO> menus, Authentication ignoredAuthentication) {
        for (MenuDTO dto : menus) {
            if (dto.getId() == null) {
                continue;
            }
            Menu menu = findMenuByIdOrThrow(dto.getId());

            if (!Objects.equals(menu.getBlog().getId(), blogId)) {
                throw new IllegalArgumentException("메뉴(ID: " + dto.getId() + ")가 해당 블로그(ID: " + blogId + ")에 속하지 않습니다.");
            }

            menu.setOrderIndex(dto.getOrderIndex());

            Menu parent = null;
            if (dto.getParentId() != null) {
                parent = findMenuByIdOrThrow(dto.getParentId());
                validateParentMenu(parent, blogId);
            }
            menu.setParent(parent);
        }
    }

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwnerByMenuId(#menuId)")
    public MenuDTO updateMenu(Long menuId, MenuDTO dto, Authentication ignoredAuthentication) {
        Menu menu = findMenuByIdOrThrow(menuId);

        if (dto.getBlogId() != null && !Objects.equals(menu.getBlog().getId(), dto.getBlogId())) {
            throw new IllegalArgumentException("메뉴의 블로그 소속(blogId)은 변경할 수 없습니다.");
        }

        if (dto.getName() != null) {
            menu.setName(dto.getName());
        }
        if (dto.getOrderIndex() != null) {
            menu.setOrderIndex(dto.getOrderIndex());
        }

        Long currentParentId = (menu.getParent() != null) ? menu.getParent().getId() : null;
        if (!Objects.equals(currentParentId, dto.getParentId())) {
            Menu newParent = null;
            if (dto.getParentId() != null) {
                newParent = findMenuByIdOrThrow(dto.getParentId());
                validateParentMenu(newParent, menu.getBlog().getId());
            }
            menu.setParent(newParent);
        }

        Menu updatedMenu = menuRepository.save(menu);
        return menuMapper.toDto(updatedMenu);
    }

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwnerByMenuId(#menuId)")
    public void deleteMenu(Long menuId, Authentication ignoredAuthentication) {
        Menu menu = findMenuByIdOrThrow(menuId);
        menuRepository.delete(menu);
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getMenusByBlogId(Long blogId) {
        List<Menu> menus = menuRepository.findByBlogIdOrderByOrderIndexAsc(blogId);
        // Consider using MenuMapper if it maps all necessary fields including subMenuIds
        List<MenuDTO> menuDtos = new ArrayList<>();
        for (Menu menuEntity : menus) {
            MenuDTO dto = new MenuDTO();
            dto.setId(menuEntity.getId());
            dto.setName(menuEntity.getName());
            dto.setOrderIndex(menuEntity.getOrderIndex());
            dto.setParentId(menuEntity.getParent() != null ? menuEntity.getParent().getId() : null);

            if (menuEntity.getSubMenus() != null && !menuEntity.getSubMenus().isEmpty()) {
                dto.setSubMenuIds(menuEntity.getSubMenus().stream()
                        .map(Menu::getId)
                        .collect(Collectors.toList()));
            } else {
                dto.setSubMenuIds(new ArrayList<>());
            }
            menuDtos.add(dto);
        }
        return menuDtos;
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getAllMenus() {
        List<Menu> menus = menuRepository.findAll();
        return menuMapper.toDto(menus);
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getSubMenus(Long parentMenuId) {
        List<Menu> subMenus = menuRepository.findByParentId(parentMenuId);
        return menuMapper.toDto(subMenus);
    }

    private Blog findBlogByIdOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID: " + blogId + ")"));
    }

    private Menu findMenuByIdOrThrow(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("메뉴를 찾을 수 없습니다 (ID: " + menuId + ")"));
    }

    private void validateParentMenu(Menu parent, Long expectedBlogId) {
        if (!Objects.equals(parent.getBlog().getId(), expectedBlogId)) {
            throw new IllegalArgumentException("부모 메뉴(ID: " + parent.getId() + ")가 대상 블로그(ID: " + expectedBlogId + ")에 속하지 않습니다.");
        }
    }

    private int calculateNextOrderIndex(Long blogId) {
        return menuRepository.findMaxOrderIndexByBlogId(blogId)
                .map(maxOrder -> maxOrder + 1)
                .orElse(0);
    }
}
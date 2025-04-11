package com.glowrise.web;

import com.glowrise.service.MenuService;
import com.glowrise.service.dto.MenuDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuDTO dto, Authentication authentication) {
        MenuDTO createdMenu = menuService.createMenu(dto, authentication);
        return ResponseEntity.ok(createdMenu);
    }

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<List<MenuDTO>> getMenusByBlogId(@PathVariable Long blogId) {
        List<MenuDTO> menus = menuService.getMenusByBlogId(blogId);
        return ResponseEntity.ok(menus);
    }

    @PutMapping("/{blogId}/order")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateMenuOrder(
            @PathVariable Long blogId,
            @RequestBody List<MenuDTO> menus,
            Authentication authentication) {
        menuService.updateMenuOrder(blogId, menus, authentication);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{menuId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MenuDTO> updateMenu(
            @PathVariable Long menuId,
            @RequestBody MenuDTO dto,
            Authentication authentication) {
        MenuDTO updatedMenu = menuService.updateMenu(menuId, dto, authentication);
        return ResponseEntity.ok(updatedMenu);
    }

    @DeleteMapping("/{menuId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteMenu(
            @PathVariable Long menuId,
            Authentication authentication) {
        menuService.deleteMenu(menuId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MenuDTO>> getAllMenus() {
        List<MenuDTO> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    @GetMapping("/{menuId}/submenus")
    public ResponseEntity<List<MenuDTO>> getSubMenus(@PathVariable Long menuId) {
        List<MenuDTO> subMenus = menuService.getSubMenus(menuId);
        return ResponseEntity.ok(subMenus);
    }
}
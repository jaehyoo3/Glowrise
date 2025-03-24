package com.glowrise.web;

import com.glowrise.service.MenuService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.MenuDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuDTO dto, Authentication authentication) {
        checkAuthentication(authentication);
        MenuDTO createdMenu = menuService.createMenu(dto, authentication);
        return ResponseEntity.ok(createdMenu);
    }

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<List<MenuDTO>> getMenusByBlogId(@PathVariable Long blogId) {
        List<MenuDTO> menus = menuService.getMenusByBlogId(blogId);
        return ResponseEntity.ok(menus);
    }

    @PutMapping("/{blogId}/order")
    public ResponseEntity<Void> updateMenuOrder(
            @PathVariable Long blogId,
            @RequestBody List<MenuDTO> menus,
            Authentication authentication) {
        checkAuthentication(authentication);
        log.info("Received request to update menu order for blogId: {}, menus: {}", blogId, menus);
        menuService.updateMenuOrder(blogId, menus, authentication);
        log.info("Successfully updated menu order for blogId: {}", blogId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuDTO> updateMenu(
            @PathVariable Long menuId,
            @RequestBody MenuDTO dto,
            Authentication authentication) {
        checkAuthentication(authentication);
        MenuDTO updatedMenu = menuService.updateMenu(menuId, dto, authentication);
        return ResponseEntity.ok(updatedMenu);
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(
            @PathVariable Long menuId,
            Authentication authentication) {
        checkAuthentication(authentication);
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

    private void checkAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}
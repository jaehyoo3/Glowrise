package com.glowrise.web;

import com.glowrise.service.MenuService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.MenuDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuDTO> createMenu(@RequestBody MenuDTO dto) {
        MenuDTO createdMenu = menuService.createMenu(dto);
        return ResponseEntity.ok(createdMenu);
    }

    @PutMapping("/{menuId}")
    public ResponseEntity<MenuDTO> updateMenu(@PathVariable Long menuId, @RequestBody MenuDTO dto) {
        MenuDTO updatedMenu = menuService.updateMenu(menuId, dto);
        return ResponseEntity.ok(updatedMenu);
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MenuDTO>> getAllMenus() {
        List<MenuDTO> menus = menuService.getAllMenus();
        return ResponseEntity.ok(menus);
    }

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<List<MenuDTO>> getMenusByBlogId(@PathVariable Long blogId) {
        List<MenuDTO> menus = menuService.getMenusByBlogId(blogId);
        return ResponseEntity.ok(menus);
    }

    @GetMapping("/{menuId}/submenus")
    public ResponseEntity<List<MenuDTO>> getSubMenus(@PathVariable Long menuId) {
        List<MenuDTO> subMenus = menuService.getSubMenus(menuId);
        return ResponseEntity.ok(subMenus);
    }

    @GetMapping("/check-url")
    public ResponseEntity<Boolean> checkUrlAvailability(@RequestParam Long blogId, @RequestParam String url) {
        boolean available = menuService.isUrlAvailable(blogId, url);
        return ResponseEntity.ok(available);
    }

}

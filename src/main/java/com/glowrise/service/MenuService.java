package com.glowrise.service;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Menu;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.MenuRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.MenuDTO;
import com.glowrise.service.mapper.MenuMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    // --- 쓰기 작업 (Write Operations) ---

    @Transactional
    public MenuDTO createMenu(MenuDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Blog blog = findBlogByIdOrThrow(dto.getBlogId());
        ensureBlogOwnership(blog, userId, "메뉴를 생성할"); // 소유권 확인

        // 매퍼를 사용하여 엔티티 생성 (Blog 관계는 자동 매핑됨)
        Menu menu = menuMapper.toEntity(dto);
        // menu.setBlog(blog); // 매퍼가 처리하므로 중복 설정 불필요

        // 부모 메뉴 처리 및 유효성 검사
        if (dto.getParentId() != null) {
            Menu parent = findMenuByIdOrThrow(dto.getParentId());
            validateParentMenu(parent, blog.getId()); // 부모 메뉴 유효성 검사
            menu.setParent(parent); // parent 관계 설정 (매퍼가 처리하지 않는 경우 필요)
        } else {
            menu.setParent(null);
        }

        // 정렬 순서 설정 (null인 경우)
        if (menu.getOrderIndex() == null) {
            menu.setOrderIndex(calculateNextOrderIndex(blog.getId()));
        }

        Menu savedMenu = menuRepository.save(menu);
        log.info("블로그 ID {}에 메뉴 생성 성공 (메뉴 ID: {})", blog.getId(), savedMenu.getId());
        return menuMapper.toDto(savedMenu); // DTO로 변환하여 반환
    }

    @Transactional
    public void updateMenuOrder(Long blogId, List<MenuDTO> menus, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Blog blog = findBlogByIdOrThrow(blogId);
        ensureBlogOwnership(blog, userId, "메뉴 순서를 수정할"); // 소유권 확인

        log.info("블로그 ID {}의 메뉴 순서 업데이트 시작 ({}개 항목)", blogId, menus.size());
        for (MenuDTO dto : menus) {
            if (dto.getId() == null) {
                log.warn("메뉴 ID가 없는 DTO 건너뜀: {}", dto);
                continue;
            }
            Menu menu = findMenuByIdOrThrow(dto.getId());

            // 메뉴가 해당 블로그 소속인지 확인
            if (!Objects.equals(menu.getBlog().getId(), blogId)) {
                throw new IllegalArgumentException("메뉴(ID: " + dto.getId() + ")가 해당 블로그(ID: " + blogId + ")에 속하지 않습니다.");
            }

            // 정렬 순서 업데이트
            menu.setOrderIndex(dto.getOrderIndex());

            // 부모 메뉴 업데이트
            Menu parent = null;
            if (dto.getParentId() != null) {
                parent = findMenuByIdOrThrow(dto.getParentId());
                validateParentMenu(parent, blogId); // 부모 메뉴 유효성 검사
            }
            menu.setParent(parent);

            // menuRepository.save(menu); // 루프 내에서 저장하지 않음! @Transactional 종료 시 JPA가 처리
            log.debug("메뉴 ID {}의 순서 ({}), 부모 ({}) 업데이트 예정", dto.getId(), dto.getOrderIndex(), dto.getParentId());
        }
        // @Transactional 어노테이션에 의해 변경된 모든 메뉴 엔티티가 메서드 종료 시 저장됨
        log.info("블로그 ID {}의 메뉴 순서 업데이트 완료", blogId);
    }

    @Transactional
    public MenuDTO updateMenu(Long menuId, MenuDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Menu menu = findMenuByIdOrThrow(menuId);
        ensureBlogOwnership(menu.getBlog(), userId, "메뉴를 수정할"); // 소유권 확인

        // 블로그 ID 변경 시도 방지 (메뉴 이동은 별도 기능으로 고려)
        if (!Objects.equals(menu.getBlog().getId(), dto.getBlogId())) {
            throw new IllegalArgumentException("메뉴의 블로그 소속(blogId)은 변경할 수 없습니다.");
        }

        // 이름 업데이트
        if (dto.getName() != null) {
            menu.setName(dto.getName());
        }
        // 정렬 순서 업데이트
        if (dto.getOrderIndex() != null) {
            menu.setOrderIndex(dto.getOrderIndex());
        }

        // 부모 메뉴 업데이트 (변경 시에만 처리)
        Long currentParentId = (menu.getParent() != null) ? menu.getParent().getId() : null;
        if (!Objects.equals(currentParentId, dto.getParentId())) {
            Menu newParent = null;
            if (dto.getParentId() != null) {
                newParent = findMenuByIdOrThrow(dto.getParentId());
                validateParentMenu(newParent, menu.getBlog().getId()); // 부모 유효성 검사
            }
            menu.setParent(newParent);
        }

        // partialUpdate 사용 가능 시 대체 고려
        // menuMapper.partialUpdate(menu, dto); // 매퍼에 partialUpdate 구현 필요

        Menu updatedMenu = menuRepository.save(menu); // 변경사항 저장
        log.info("메뉴 업데이트 성공 (메뉴 ID: {})", menuId);
        return menuMapper.toDto(updatedMenu);
    }

    @Transactional
    public void deleteMenu(Long menuId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Menu menu = findMenuByIdOrThrow(menuId);
        ensureBlogOwnership(menu.getBlog(), userId, "메뉴를 삭제할"); // 소유권 확인

        // TODO: 하위 메뉴 처리 정책 결정 필요 (예: 같이 삭제, null로 변경 등)
        // 현재는 하위 메뉴가 있으면 외래 키 제약 조건 등으로 인해 삭제가 실패할 수 있음

        menuRepository.delete(menu);
        log.info("메뉴 삭제 성공 (메뉴 ID: {})", menuId);
    }

    // --- 읽기 작업 (Read Operations) ---

    @Transactional(readOnly = true)
    public List<MenuDTO> getMenusByBlogId(Long blogId) {
        // DB에서 Menu 엔티티 목록 조회 (정렬 포함)
        List<Menu> menus = menuRepository.findByBlogIdOrderByOrderIndexAsc(blogId);

        // 수동으로 List<Menu> -> List<MenuDTO> 변환
        List<MenuDTO> menuDtos = new ArrayList<>();
        for (Menu menuEntity : menus) {
            MenuDTO dto = new MenuDTO();
            dto.setId(menuEntity.getId());
            dto.setName(menuEntity.getName()); // 원본 이름 (들여쓰기 없음)
            dto.setOrderIndex(menuEntity.getOrderIndex());
            dto.setParentId(menuEntity.getParent() != null ? menuEntity.getParent().getId() : null);

            // subMenuIds 설정 (null 체크 후 ID 추출 또는 빈 리스트)
            if (menuEntity.getSubMenus() != null && !menuEntity.getSubMenus().isEmpty()) {
                dto.setSubMenuIds(menuEntity.getSubMenus().stream()
                        .map(Menu::getId)
                        .collect(Collectors.toList()));
            } else {
                dto.setSubMenuIds(new ArrayList<>());
            }

            // blogId, postIds 설정 로직 제거됨

            menuDtos.add(dto); // 리스트에 DTO 추가
        }
        return menuDtos; // 변환된 DTO 리스트 반환
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getAllMenus() {
        // 모든 메뉴 조회 (성능 고려: 페이지네이션 또는 필요한 경우로 제한)
        List<Menu> menus = menuRepository.findAll();
        return menuMapper.toDto(menus);
    }

    @Transactional(readOnly = true)
    public List<MenuDTO> getSubMenus(Long parentMenuId) {
        // 부모 ID로 하위 메뉴 조회 (정렬 필요 시 Repository 메소드 수정)
        List<Menu> subMenus = menuRepository.findByParentId(parentMenuId);
        return menuMapper.toDto(subMenus);
    }

    // --- 비공개 헬퍼 메소드 (Private Helper Methods) ---

    /**
     * 인증 객체에서 사용자 ID를 추출합니다.
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        // 다른 서비스와 중복되므로 공통 유틸리티 클래스로 분리 고려
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }

    /**
     * ID로 Blog 엔티티를 찾거나 예외를 발생시킵니다.
     */
    private Blog findBlogByIdOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID: " + blogId + ")"));
    }

    /**
     * ID로 Menu 엔티티를 찾거나 예외를 발생시킵니다.
     */
    private Menu findMenuByIdOrThrow(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new EntityNotFoundException("메뉴를 찾을 수 없습니다 (ID: " + menuId + ")"));
    }

    /**
     * 블로그 소유권을 확인하고, 권한이 없으면 AccessDeniedException을 발생시킵니다.
     */
    private void ensureBlogOwnership(Blog blog, Long userId, String action) {
        if (blog.getUser() == null || !Objects.equals(blog.getUser().getId(), userId)) {
            log.warn("사용자 ID {}가 블로그 ID {}의 {} 권한이 없습니다.", userId, blog.getId(), action);
            throw new AccessDeniedException("해당 블로그의 " + action + " 권한이 없습니다.");
        }
    }

    /**
     * 부모 메뉴가 유효한지 (null이 아니고, 같은 블로그에 속하는지) 검사합니다.
     */
    private void validateParentMenu(Menu parent, Long expectedBlogId) {
        // parent는 이미 findMenuByIdOrThrow를 통과했으므로 null 체크 불필요
        if (!Objects.equals(parent.getBlog().getId(), expectedBlogId)) {
            throw new IllegalArgumentException("부모 메뉴(ID: " + parent.getId() + ")가 대상 블로그(ID: " + expectedBlogId + ")에 속하지 않습니다.");
        }
        // 추가: 자기 자신을 부모로 설정하는 것 방지 로직 필요 시 추가
    }

    /**
     * 다음 메뉴 순서(orderIndex)를 계산합니다.
     */
    private int calculateNextOrderIndex(Long blogId) {
        return menuRepository.findMaxOrderIndexByBlogId(blogId)
                .map(maxOrder -> maxOrder + 1) // 최대값 + 1
                .orElse(0); // 메뉴가 하나도 없으면 0부터 시작
    }
}
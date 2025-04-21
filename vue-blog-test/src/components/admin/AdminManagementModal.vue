<template>
  <PrimeDialog
      v-model:visible="localVisible"
      :closable="false"
      :header="false"
      :modal="true"
      :style="{width: '90vw', minWidth: '900px', maxWidth: '1600px', height: '90vh', minHeight: '700px'}"
      class="admin-management-modal"
      contentClass="admin-modal-content"
      @hide="onHide"
  >
    <div class="admin-layout">
      <div class="admin-topbar">
        <div class="admin-tabs">
          <div class="tab-row secondary-row">
            <div :class="{ active: activeMenuItem === 'advertisements' }" class="tab-item secondary-tab"
                 @click="activeMenuItem = 'advertisements'">
              <i class="pi pi-megaphone"></i>
              <span>광고 관리</span>
            </div>
            <div :class="{ active: activeMenuItem === 'notices' }" class="tab-item secondary-tab"
                 @click="activeMenuItem = 'notices'">
              <i class="pi pi-bell"></i>
              <span>공지사항 관리</span>
            </div>
          </div>

          <div class="tab-row primary-row">
            <div :class="{ active: activeMenuItem === 'users' }" class="tab-item" @click="activeMenuItem = 'users'">
              <i class="pi pi-users"></i>
              <span>사용자 관리</span>
            </div>
            <div :class="{ active: activeMenuItem === 'posts' }" class="tab-item" @click="activeMenuItem = 'posts'">
              <i class="pi pi-file-edit"></i>
              <span>게시글 관리</span>
            </div>
            <div :class="{ active: activeMenuItem === 'comments' }" class="tab-item"
                 @click="activeMenuItem = 'comments'">
              <i class="pi pi-comments"></i>
              <span>댓글 관리</span>
            </div>
          </div>
        </div>

        <div class="close-button" @click="hideDialog">
          <i class="pi pi-times"></i>
        </div>
      </div>

      <div class="admin-content">
        <div class="content-body">
          <keep-alive>
            <component :is="activeComponent" @close-modal="hideDialog"/>
          </keep-alive>

          <div v-if="!activeComponent" class="list-container">
            <div class="list-header">
              <div class="header-cell id-cell">ID</div>
              <div class="header-cell image-cell">이미지</div>
              <div class="header-cell title-cell">제목</div>
              <div class="header-cell url-cell">링크 URL</div>
              <div class="header-cell date-cell">시작일시</div>
              <div class="header-cell date-cell">종료일시</div>
              <div class="header-cell status-cell">순서</div>
              <div class="header-cell status-cell">상태</div>
              <div class="header-cell action-cell">관리</div>
            </div>

            <div v-if="activeMenuItem === 'advertisements'" class="list-empty">
              등록된 광고가 없습니다.
            </div>

            <template v-for="n in 20" :key="n">
              <div class="list-row">
                <div class="cell id-cell">{{ n }}</div>
                <div class="cell image-cell">-</div>
                <div class="cell title-cell">샘플 항목 {{ n }}</div>
                <div class="cell url-cell">https://example.com</div>
                <div class="cell date-cell">2025-04-01</div>
                <div class="cell date-cell">2025-04-30</div>
                <div class="cell status-cell">{{ n }}</div>
                <div class="cell status-cell">활성</div>
                <div class="cell action-cell">
                  <button class="action-btn">관리</button>
                </div>
              </div>
            </template>
          </div>

          <div class="pagination-container">
            <div class="pagination">
              <button :disabled="currentPage === 1" class="pagination-button" @click="goToPage(currentPage - 1)">
                <i class="pi pi-chevron-left"></i>
              </button>

              <template v-for="page in displayedPages" :key="page">
                <button
                    v-if="page !== '...'"
                    :class="{ active: currentPage === page }"
                    class="pagination-button"
                    @click="goToPage(page)"
                >
                  {{ page }}
                </button>
                <span v-else class="pagination-ellipsis">{{ page }}</span>
              </template>

              <button :disabled="currentPage === totalPages" class="pagination-button"
                      @click="goToPage(currentPage + 1)">
                <i class="pi pi-chevron-right"></i>
              </button>
            </div>
            <div class="page-info">
              {{ (currentPage - 1) * itemsPerPage + 1 }} - {{ Math.min(currentPage * itemsPerPage, totalItems) }} /
              {{ totalItems }} 항목
            </div>
          </div>
        </div>
      </div>
    </div>
  </PrimeDialog>
</template>

<script setup>
import {computed, ref} from 'vue';

// 관리 메뉴별 컨텐츠 컴포넌트 임포트
import AdminAdvertisements from './AdminAdvertisements.vue';
// 플레이스홀더 컴포넌트 임포트 (추후 실제 컴포넌트로 교체)
import AdminUsersPlaceholder from './placeholders/AdminUsersPlaceholder.vue';
import AdminPostsPlaceholder from './placeholders/AdminPostsPlaceholder.vue';
import AdminCommentsPlaceholder from './placeholders/AdminCommentsPlaceholder.vue';
import AdminNoticesPlaceholder from './placeholders/AdminNoticesPlaceholder.vue';

// Props, Emits 정의
const props = defineProps({visible: Boolean});
const emit = defineEmits(['update:visible']);

// 모달 표시 상태 computed
const localVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
});

// 현재 활성화된 메뉴 아이템 (기본값: 광고 관리)
const activeMenuItem = ref('advertisements');

// 활성화된 메뉴에 따라 표시할 컴포넌트 매핑
const activeComponent = computed(() => {
  switch (activeMenuItem.value) {
    case 'advertisements':
      return AdminAdvertisements;
    case 'users':
      return AdminUsersPlaceholder;
    case 'posts':
      return AdminPostsPlaceholder;
    case 'comments':
      return AdminCommentsPlaceholder;
    case 'notices':
      return AdminNoticesPlaceholder;
    default:
      return null;
  }
});

// 모달 닫기 함수
const hideDialog = () => {
  localVisible.value = false;
};

// 모달이 완전히 숨겨진 후 이벤트 처리
const onHide = () => {
  // 필요 시 추가 로직
};

// 페이지네이션 관련 상태
const currentPage = ref(1);
const itemsPerPage = 20;
const totalItems = ref(123); // 샘플 데이터, 실제 데이터로 교체 필요
const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage));

// 페이지 이동 함수
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

// 표시할 페이지 번호 계산
const displayedPages = computed(() => {
  const pages = [];
  const maxVisiblePages = 5;

  if (totalPages.value <= maxVisiblePages) {
    for (let i = 1; i <= totalPages.value; i++) {
      pages.push(i);
    }
  } else {
    pages.push(1);
    let startPage = Math.max(2, currentPage.value - 1);
    let endPage = Math.min(totalPages.value - 1, currentPage.value + 1);
    if (currentPage.value <= 3) {
      endPage = Math.min(totalPages.value - 1, 4);
    } else if (currentPage.value >= totalPages.value - 2) {
      startPage = Math.max(2, totalPages.value - 3);
    }
    if (startPage > 2) {
      pages.push('...');
    }
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    if (endPage < totalPages.value - 1) {
      pages.push('...');
    }
    pages.push(totalPages.value);
  }
  return pages;
});
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  overflow: hidden;
  background-color: #f5f5f5;
}

.admin-topbar {
  display: flex;
  align-items: flex-end;
  height: 100px;
  background-color: #444;
  box-shadow: none;
  z-index: 2;
  padding: 0 20px;
  position: relative;
}

.admin-tabs {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: flex-end;
  height: 100%;
  flex-grow: 1;
}

.tab-row {
  display: flex;
  position: relative;
  width: 100%;
}

.primary-row {
  padding-left: 20px;
  z-index: 2;
}

.secondary-row {
  padding-left: 100px;
  z-index: 1;
  margin-bottom: -8px;
  position: relative;
}

/* 공통 탭 스타일 */
.tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 1.5rem;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, font-weight 0.2s ease;
  font-size: 0.95rem;
  border-top-left-radius: 6px;
  border-top-right-radius: 6px;
  margin-right: 0;
  position: relative;
  box-shadow: none;
  /* border-right: 1px solid #666; 제거 */
  border-right: none; /* 구분선 제거 */
  background-color: #555;
  color: #ccc;
}

/* 제거: .tab-item:last-child */

.tab-item:hover {
  background-color: #6a6a6a;
  color: #fff;
}

/* 모든 활성 탭 스타일 통일 */
.tab-item.active {
  background-color: #fff;
  color: #444;
  font-weight: 500;
}

/* 각 row 별 높이 설정 */
.primary-row .tab-item {
  height: 40px;
}

.secondary-row .tab-item {
  height: 45px;
}

/* 활성 탭 높이도 각 row 기본 높이와 동일하게 */
.primary-row .tab-item.active {
  height: 40px;
}

.secondary-row .tab-item.active {
  height: 45px;
}


.tab-item i {
  margin-right: 0.7rem;
  font-size: 1rem;
}

.close-button {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 3;
  position: absolute;
  top: 10px;
  right: 20px;
}

.close-button:hover {
  color: #fff;
}

.close-button i {
  font-size: 1.2rem;
}

.admin-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  height: calc(100% - 100px);
  overflow: hidden;
}

.content-body {
  flex-grow: 1;
  padding: 1rem;
  overflow: auto;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* Action buttons */
.action-buttons {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 1rem;
}

.add-button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: 0.9rem;
}

.add-button i {
  margin-right: 6px;
}

/* List container */
.list-container {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: auto;
}

.list-header {
  display: flex;
  background-color: #f8f8f8;
  border-bottom: 1px solid #e0e0e0;
  font-weight: 500;
  color: #555;
  position: sticky;
  top: 0;
  z-index: 1;
}

.header-cell, .cell {
  padding: 12px 15px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cell.title-cell {
  justify-content: flex-start;
}

.id-cell {
  width: 60px;
}

.image-cell {
  width: 100px;
}

.title-cell {
  flex-grow: 1;
  text-align: left;
}

.url-cell {
  width: 180px;
}

.date-cell {
  width: 120px;
}

.status-cell {
  width: 80px;
}

.action-cell {
  width: 80px;
}

.list-row {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  height: 50px;
}

.list-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #888;
  font-size: 0.95rem;
}

.action-btn {
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 0.8rem;
  cursor: pointer;
}

/* Pagination styles */
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1.5rem;
  padding-top: 1rem;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-button {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #444;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.9rem;
}

.pagination-button:hover:not(:disabled) {
  background-color: #e0e0e0;
}

.pagination-button.active {
  background-color: #555;
  color: #fff;
  border-color: #555;
}

.pagination-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-ellipsis {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.page-info {
  margin-left: 1rem;
  color: #666;
  font-size: 0.9rem;
}

/* PrimeDialog 스타일 오버라이드 */
:deep(.admin-management-modal) {
  border-radius: 8px;
  overflow: hidden;
  border: none;
}

:deep(.admin-management-modal .p-dialog-header) {
  display: none !important;
}

:deep(.admin-modal-content.p-dialog-content) {
  padding: 0;
  height: 100%;
  display: flex;
  overflow: hidden;
  border-radius: 8px;
}

:deep(.p-dialog-header-icons) {
  display: none !important;
}

:deep(.p-component) {
  font-family: 'Noto Sans KR', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}
</style>
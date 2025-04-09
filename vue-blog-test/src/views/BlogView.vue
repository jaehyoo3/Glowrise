<template>
  <div class="blog-view">
    <div class="container">
      <div v-if="isLoading" class="loading-state">
        <span>로딩 중...</span>
      </div>
      <div v-else-if="blog" class="blog-content">
        <div class="main-grid">
          <div class="blog-menu">
            <h3>메뉴</h3>
            <ul class="menu-list">
              <li class="menu-item">
                <router-link :to="`/${blog.url}`">전체글 보기</router-link>
              </li>
              <li v-for="menu in rootMenus" :key="menu.id" class="menu-item">
                <router-link :to="`/${blog.url}/${menu.id}`">{{ menu.name }}</router-link>
                <ul v-if="getSubMenus(menu.id).length > 0" class="submenu-list">
                  <li v-for="submenu in getSubMenus(menu.id)" :key="submenu.id" class="submenu-item">
                    <router-link :to="`/${blog.url}/${submenu.id}`">{{ submenu.name }}</router-link>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
          <div class="blog-main">
            <div class="blog-header">
              <div class="blog-title-container">
                <h1 class="blog-title">{{ blog.title }}</h1>
                <div v-if="isOwner" class="blog-management-icons">
                  <router-link :to="`/blog/edit/${blog.id}`" class="icon-btn">
                    <i class="fas fa-cog"></i>
                  </router-link>
                </div>
              </div>
              <p class="blog-description">{{ blog.description }}</p>
            </div>

            <div class="blog-section">
              <div class="section-header">
                <h2 class="section-title">
                  {{ selectedMenu ? selectedMenu.name : '전체 게시글' }}
                </h2>
                <div class="section-actions">
                  <input
                      v-model="searchKeyword"
                      @keyup.enter="searchPosts"
                      placeholder="제목 또는 내용으로 검색"
                      class="search-input"
                  >
                  <button @click="searchPosts" class="btn-search">검색</button>
                  <div v-if="isOwner && !isParentMenu" class="write-actions">
                    <router-link
                        :to="`/${blog.url}/post/create${selectedMenu ? '?menuId=' + selectedMenu.id : ''}`"
                        class="btn-primary"
                    >
                      글쓰기
                    </router-link>
                    <select v-model="pageSize" @change="changePageSize" class="page-size-select">
                      <option value="20">20개</option>
                      <option value="30">30개</option>
                      <option value="50">50개</option>
                    </select>
                  </div>
                </div>
              </div>

              <div class="post-list">
                <div v-if="!posts.content?.length" class="no-posts">
                  게시글이 없습니다.
                </div>
                <ul v-else class="posts">
                  <li v-for="post in posts.content" :key="post.id" class="post-item">
                    <div class="post-checkbox" v-if="isOwner">
                      <input
                          type="checkbox"
                          v-model="selectedPosts"
                          :value="post.id"
                      >
                    </div>
                    <div class="post-content">
                      <div class="post-title-container">
                        <router-link
                            :to="{
                              path: `/${blog.url}/${post.menuId}/${post.id}`,
                              query: { fromAll: !$route.params.menuId ? 'true' : undefined }
                            }"
                            class="post-link"
                        >
                          {{ post.title }} [{{ post.commentCount || 0 }}]
                        </router-link>
                        <i v-if="post.hasAttachments || post.fileCount > 0" class="fas fa-paperclip file-icon"></i>
                      </div>
                      <span class="post-meta">
                        {{ formatDate(post.updatedAt) }} 조회수: {{ post.viewCount || 0 }}
                      </span>
                    </div>
                  </li>
                </ul>

                <!-- 페이지네이션 조건 제거 -->
                <div class="pagination">
                  <button
                      @click="changePage(currentPage - 1)"
                      :disabled="currentPage === 0"
                      class="pagination-btn"
                  >
                    &lt;
                  </button>
                  <input
                      type="text"
                      v-model.number="currentPageInput"
                      @keyup.enter="updatePage"
                      class="pagination-input"
                  >
                  <span class="pagination-text"> / {{ posts.totalPages }}</span>
                  <button
                      @click="changePage(currentPage + 1)"
                      :disabled="currentPage === posts.totalPages - 1"
                      class="pagination-btn"
                  >
                    &gt;
                  </button>
                </div>

                <div v-if="isOwner && posts.content?.length > 0" class="bulk-delete-section">
                  <div class="select-all-checkbox">
                    <input
                        type="checkbox"
                        :checked="selectedPosts.length === posts.content?.length"
                        @change="toggleSelectAll"
                    >
                    <span>전체 선택</span>
                  </div>
                  <button
                      @click="bulkDeletePosts"
                      class="btn-danger bulk-delete-btn"
                      :disabled="selectedPosts.length === 0"
                  >
                    선택 삭제
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="no-blog">
        <h1>블로그를 찾을 수 없습니다.</h1>
        <router-link to="/" class="btn-primary">홈으로 돌아가기</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import {reactive} from 'vue'; // setup 부분 유지
import {mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import

export default {
  name: 'BlogView',
  props: {
    blogUrl: String, // 라우터 prop
    menuId: String,  // 라우터 prop (선택적)
  },
  // setup()은 reactive posts 선언을 위해 그대로 둡니다.
  setup() {
    const posts = reactive({content: [], totalPages: 0});
    return {posts};
  },
  data() {
    return {
      blog: null,      // 현재 보고 있는 블로그 정보
      menus: [],       // 현재 블로그의 메뉴 목록
      isLoading: true, // 블로그/메뉴 로딩 상태
      isOwner: false,  // 현재 로그인 사용자가 블로그 소유주인지 여부
      selectedMenu: null, // 현재 선택된 메뉴 객체
      currentMenuId: null, // 현재 URL의 메뉴 ID
      // --- 삭제: currentUser 로컬 상태 제거 ---
      selectedPosts: [], // 선택된 게시글 ID 목록 (삭제용)
      currentPage: 0,    // 현재 게시글 페이지 (0부터 시작)
      currentPageInput: 1, // 페이지 입력 필드용 (1부터 시작)
      pageSize: 10,      // 페이지 당 게시글 수 (기존 20에서 줄임, 필요시 조정)
      searchKeyword: '', // 검색어
    };
  },
  computed: {
    // --- Vuex Getters 매핑 ---
    ...mapGetters(['isLoggedIn', 'userId']), // 로그인 상태 및 사용자 ID
    // ------------------------

    // 메뉴 계산 속성 (기존과 동일)
    rootMenus() {
      return this.getRootMenus();
    },
    isParentMenu() {
      if (!this.selectedMenu) return false;
      return this.menus.some(menu => menu.parentId === this.selectedMenu.id);
    },
  },
  watch: {
    // 라우트의 menuId 변경 감지 (기존과 동일)
    '$route.params.menuId'(newMenuId) {
      this.currentMenuId = newMenuId;
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.searchKeyword = ''; // 메뉴 변경 시 검색어 초기화
      this.loadPosts(); // 게시글 다시 로드
    },
    // 페이지 번호 변경 감지 (기존과 동일)
    currentPage(newPage) {
      this.currentPageInput = newPage + 1;
    },
    // --- Vuex: 로그인 상태 변경 감지 (선택적) ---
    // 로그인 상태가 변경되면 isOwner를 다시 계산해야 할 수 있음
    isLoggedIn() {
      this.updateOwnership();
    }
    // -----------------------------------------
  },
  async created() {
    console.log('BlogView: created hook');
    // 라우터 파라미터에서 초기 메뉴 ID 설정
    this.currentMenuId = this.menuId || this.$route.params.menuId;
    // 블로그 및 메뉴 정보 로드 (내부에서 소유권 확인 로직 호출)
    await this.loadBlogAndMenus();
    // 게시글 로드 (블로그 로딩 성공 시에만)
    if (this.blog) {
      await this.loadPosts();
    }
    // 페이지 입력 필드 초기화
    this.currentPageInput = this.currentPage + 1;
  },
  methods: {
    async loadBlogAndMenus() {
      console.log('BlogView: loadBlogAndMenus 시작');
      this.isLoading = true;
      try {
        // 라우터 prop 또는 파라미터에서 블로그 URL 가져오기
        const blogUrl = this.blogUrl || this.$route.params.blogUrl;
        if (!blogUrl) throw new Error("Blog URL not found in route.");

        // authService를 통해 특정 블로그 정보 가져오기 (변경 없음)
        console.log(`BlogView: 블로그 정보 로드 중 (URL: ${blogUrl})`);
        const blog = await authService.getBlogByUrl(blogUrl);
        this.blog = blog; // 컴포넌트 상태에 저장

        if (blog && blog.id) {
          console.log(`BlogView: 블로그 로드 성공 (ID: ${blog.id}), 메뉴 로드 시작`);
          // authService를 통해 메뉴 정보 가져오기 (변경 없음)
          this.menus = await authService.getMenusByBlogId(blog.id);
          console.log(`BlogView: 메뉴 로드 완료 (${this.menus.length}개)`);

          // 현재 라우트의 메뉴 ID로 selectedMenu 설정 (변경 없음)
          const routeMenuId = this.currentMenuId || this.$route.params.menuId;
          if (routeMenuId) {
            this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
          }

          // --- Vuex: 소유권 확인 로직 변경 ---
          this.updateOwnership(); // 별도 함수로 분리
          // ---------------------------------

        } else {
          // blog 정보가 제대로 오지 않은 경우 (null 또는 id 없음)
          console.warn("BlogView: 유효하지 않은 블로그 데이터 수신");
          this.blog = null;
          this.menus = [];
          this.$router.replace('/404'); // 404 페이지로 이동
        }
      } catch (error) {
        console.error('BlogView: 블로그 또는 메뉴 로딩 실패:', error);
        if (error.response?.status === 404 || error.message.includes("Blog URL not found")) {
          this.$router.replace('/404'); // 404 에러 시 404 페이지로
        } else {
          // 기타 에러 처리 (예: 에러 메시지 표시)
          this.blog = null;
          this.menus = [];
        }
      } finally {
        this.isLoading = false;
        console.log('BlogView: loadBlogAndMenus 종료');
      }
    },

    // --- Vuex: 소유권 확인 로직 분리 ---
    updateOwnership() {
      if (this.blog && this.blog.userId && this.isLoggedIn && this.userId) {
        this.isOwner = this.blog.userId === this.userId;
        console.log(`BlogView: 소유권 확인 완료. isOwner: ${this.isOwner}`);
      } else {
        this.isOwner = false;
        console.log(`BlogView: 소유권 확인 - 비로그인 또는 정보 부족. isOwner: false`);
      }
    },
    // ----------------------------------

    async loadPosts() {
      // 게시글 로딩 로직
      if (!this.blog || !this.blog.id) {
        console.log("BlogView: 블로그 정보가 없어 게시글을 로드할 수 없음");
        this.posts.content = [];
        this.posts.totalPages = 0;
        return;
      }
      console.log(`BlogView: 게시글 로드 시작 (Menu ID: ${this.currentMenuId || '전체'}, Page: ${this.currentPage})`);
      try {
        const routeMenuId = this.currentMenuId || this.$route.params.menuId;
        const params = {
          page: this.currentPage,
          size: this.pageSize,
          sort: 'updatedAt,desc',
          searchKeyword: this.searchKeyword || undefined,
        };
        const postsData = await authService.getPostsByBlogIdAndMenuId(
            this.blog.id,
            routeMenuId || null,
            params
        );

        // selectedMenu 업데이트
        if (routeMenuId) {
          this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
        } else {
          this.selectedMenu = null;
        }

        // --- 수정: API 응답 구조에 맞춰 totalPages 접근 ---
        // postsData.page 객체가 존재하고 그 안에 totalPages가 있는지 확인
        this.posts.totalPages = postsData?.page?.totalPages || 0;
        // -----------------------------------------------

        // --- 확인: 게시글 내용 접근 경로 확인 ---
        // API 응답 구조에 따라 content 접근 경로가 다를 수 있음
        // 예시 1: 최상위 content 배열
        this.posts.content = postsData?.content || [];
        // 예시 2: Spring Data REST HAL (_embedded) 구조
        // this.posts.content = postsData?._embedded?.posts || [];
        // 실제 API 응답 전체 구조를 확인하고 위 경로 중 맞는 것을 사용하세요.
        // --------------------------------------

        console.log(`BlogView: 게시글 로드 완료 (${this.posts.content.length}개), TotalPages: ${this.posts.totalPages}`);

      } catch (error) {
        console.error("BlogView: 게시글 로딩 실패:", error);
        this.posts.content = [];
        this.posts.totalPages = 0;
      }
    },
    searchPosts() {
      // 검색 로직 (기존과 동일)
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.selectedPosts = []; // 검색 시 선택 초기화
      this.loadPosts();
    },
    changePage(newPage) {
      // 페이지 변경 로직 (기존과 동일)
      if (newPage >= 0 && newPage < this.posts.totalPages) {
        this.currentPage = newPage;
        this.selectedPosts = []; // 페이지 변경 시 선택 초기화
        this.loadPosts();
      }
    },
    updatePage() {
      // 페이지 입력 필드로 변경 로직 (기존과 동일)
      const newPage = this.currentPageInput - 1;
      if (newPage >= 0 && newPage < this.posts.totalPages) {
        this.changePage(newPage);
      } else {
        this.currentPageInput = this.currentPage + 1; // 잘못된 입력 시 현재 페이지로 복원
        if (this.posts.totalPages > 0) {
          alert(`페이지 번호는 1에서 ${this.posts.totalPages} 사이여야 합니다.`);
        } else {
          alert('게시글이 없습니다.');
        }
      }
    },
    changePageSize() {
      // 페이지 크기 변경 로직 (기존과 동일)
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.selectedPosts = [];
      this.loadPosts();
    },
    toggleSelectAll() {
      // 전체 선택 토글 (기존과 동일)
      if (this.selectedPosts.length === this.posts.content.length) {
        this.selectedPosts = [];
      } else {
        // 현재 페이지의 모든 post id 선택
        this.selectedPosts = this.posts.content.map(post => post.id);
      }
    },
    async bulkDeletePosts() {
      // 게시글 삭제 로직 (isOwner 및 userId를 스토어 getter 사용)
      if (!this.isOwner) { // isOwner computed 속성 사용
        alert('게시글을 삭제할 권한이 없습니다.');
        return;
      }
      if (this.selectedPosts.length === 0) {
        alert('삭제할 게시글을 선택해주세요.');
        return;
      }
      if (!confirm(`선택된 ${this.selectedPosts.length}개의 게시글을 정말 삭제하시겠습니까?`)) return;

      // --- Vuex: userId getter 사용 ---
      const currentUserId = this.userId;
      if (!currentUserId) {
        alert('사용자 정보를 확인할 수 없습니다.');
        return;
      }
      // -----------------------------

      try {
        // Promise.all 사용하여 병렬 삭제 (기존과 동일)
        await Promise.all(
            this.selectedPosts.map(postId =>
                authService.deletePost(postId, currentUserId) // 스토어에서 가져온 userId 사용
            )
        );
        // 삭제 성공 후 목록에서 제거 (기존과 동일)
        this.posts.content = this.posts.content.filter(post => !this.selectedPosts.includes(post.id));
        this.selectedPosts = []; // 선택 해제
        alert('선택된 게시글이 삭제되었습니다.');
        // 필요하다면 loadPosts() 다시 호출하여 페이지네이션 등 재계산
        // await this.loadPosts();
      } catch (error) {
        alert('게시글 삭제 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    formatDate(dateString) {
      // 입력값이 없으면 '날짜 없음' 반환
      if (!dateString) return '날짜 없음';

      try {
        // 입력 문자열로 Date 객체 생성
        const date = new Date(dateString);

        // 유효하지 않은 날짜 객체인지 확인
        if (isNaN(date.getTime())) {
          console.warn("formatDate: 유효하지 않은 날짜 문자열:", dateString);
          return '날짜 오류';
        }

        const now = new Date(); // 현재 시간
        const diffSeconds = Math.floor((now - date) / 1000); // 현재 시간과의 차이 (초)

        // 시간 차이에 따른 상대 시간 표시
        if (diffSeconds < 60) { // 1분 미만
          return '방금 전';
        }
        const diffMinutes = Math.floor(diffSeconds / 60); // 분 단위 차이
        if (diffMinutes < 60) { // 1시간 미만
          return `${diffMinutes}분 전`;
        }
        const diffHours = Math.floor(diffMinutes / 60); // 시간 단위 차이
        if (diffHours < 24) { // 24시간 미만
          return `${diffHours}시간 전`;
        }

        // 하루 이상 차이 나는 경우
        const diffDays = Math.floor(diffHours / 24); // 일 단위 차이
        if (diffDays === 1) { // 하루 차이
          return '어제';
        }
        if (diffDays < 7) { // 7일 미만
          return `${diffDays}일 전`;
        }

        // 7일 이상 차이 나는 경우 'YYYY.MM.DD' 형식으로 표시
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0'); // 월 (0부터 시작하므로 +1), 2자리로 포맷
        const day = String(date.getDate()).padStart(2, '0');     // 일 (2자리로 포맷)
        return `${year}.${month}.${day}`;

      } catch (e) {
        // Date 객체 생성 또는 계산 중 에러 발생 시
        console.error("formatDate 처리 중 오류:", dateString, e);
        return '날짜 형식 오류';
      }
    },
    getRootMenus() {
      // 루트 메뉴 필터링 (기존과 동일)
      if (!Array.isArray(this.menus)) return [];
      return this.menus.filter(menu => !menu.parentId).sort((a, b) => a.orderIndex - b.orderIndex); // 순서 정렬 추가
    },
    getSubMenus(parentId) {
      // 서브 메뉴 필터링 (기존과 동일)
      if (!Array.isArray(this.menus)) return [];
      return this.menus.filter(menu => menu.parentId === parentId).sort((a, b) => a.orderIndex - b.orderIndex); // 순서 정렬 추가
    },
  },
};
</script>

<style scoped>
.blog-view {
  background-color: #f8f9fa;
  min-height: 100vh;
  color: #333;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.loading-state {
  text-align: center;
  padding: 2rem;
  font-size: 1.2rem;
}

.main-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 2rem;
}

.blog-menu,
.blog-main {
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.blog-header {
  margin-bottom: 2rem;
}

.blog-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  margin-bottom: 0.5rem;
}

.blog-description {
  color: #666;
  font-size: 1rem;
}

.section-title {
  border-bottom: 2px solid #000;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}

.menu-list {
  list-style: none;
  padding: 0;
}

.menu-item,
.submenu-item {
  margin-bottom: 0.5rem;
}

.menu-item a,
.submenu-item a {
  text-decoration: none;
  color: #333;
  transition: color 0.3s ease;
}

.menu-item a:hover,
.submenu-item a:hover {
  color: #666;
}

.submenu-list {
  list-style: none;
  padding-left: 1rem;
  margin-top: 0.5rem;
}

.btn-primary,
.btn-danger {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  text-align: center;
}

.btn-primary {
  background-color: #000;
  color: white;
}

.btn-danger {
  background-color: #dc3545;
  color: white;
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
}

.post-list {
  margin-top: 1rem;
}

.posts {
  list-style: none;
  padding: 0;
}

.post-item {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #e5e5e5;
}

.post-checkbox {
  margin-right: 1rem;
}

.post-content {
  flex-grow: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-title-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.post-link {
  text-decoration: none;
  color: #000;
}

.file-icon {
  color: #6c757d;
  font-size: 0.8rem;
}

.post-meta {
  color: #666;
  font-size: 0.9rem;
  white-space: nowrap;
}

.no-posts {
  text-align: center;
  color: #666;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.no-blog {
  text-align: center;
  padding: 2rem;
}

@media (max-width: 768px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}

.blog-title-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.blog-management-icons {
  display: flex;
  align-items: center;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.search-input {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  width: 200px;
}

.btn-search {
  padding: 0.5rem 1rem;
  background-color: #666;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.btn-search:hover {
  background-color: #888;
}

.write-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  color: #333;
  transition: all 0.3s ease;
  text-decoration: none;
}

.icon-btn:hover {
  background-color: #f0f0f0;
}

.icon-btn i {
  font-size: 1.2rem;
}

/* 페이지네이션 스타일 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.pagination-btn {
  padding: 0.5rem 1rem;
  background-color: #000;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.pagination-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.pagination-input {
  width: 50px;
  padding: 0.5rem;
  text-align: center;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
}

.pagination-text {
  font-size: 1rem;
  color: #666;
}

.page-size-select {
  margin-left: 1rem;
  padding: 0.5rem;
  border-radius: 4px;
  border: 1px solid #ccc;
  background-color: white;
  cursor: pointer;
}

.bulk-delete-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #f8f9fa;
  border-top: 1px solid #e5e5e5;
}

.select-all-checkbox {
  display: flex;
  align-items: center;
}

.select-all-checkbox span {
  margin-left: 0.5rem;
}

.bulk-delete-btn {
  padding: 0.5rem 1rem;
}

.bulk-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
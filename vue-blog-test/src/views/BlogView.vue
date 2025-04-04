<template>
  <div class="blog-view">
    <NavBar />
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
import {reactive} from 'vue';
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'BlogView',
  components: { NavBar },
  props: {
    blogUrl: String,
    menuId: String,
  },
  setup() {
    const posts = reactive({content: [], totalPages: 0});
    return {posts};
  },
  data() {
    return {
      blog: null,
      menus: [],
      isLoading: true,
      isOwner: false,
      selectedMenu: null,
      currentMenuId: null,
      currentUser: null,
      selectedPosts: [],
      currentPage: 0,
      currentPageInput: 1, // 텍스트 박스에 표시될 값 (1부터 시작)
      pageSize: 20,
      searchKeyword: '',
    };
  },
  async created() {
    this.currentMenuId = this.menuId || this.$route.params.menuId;
    await this.loadBlogAndMenus();
    await this.loadPosts();
    this.currentPageInput = this.currentPage + 1; // 초기값 설정
  },
  watch: {
    '$route.params.menuId'(newMenuId) {
      this.currentMenuId = newMenuId;
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.searchKeyword = '';
      this.loadPosts();
    },
    currentPage(newPage) {
      this.currentPageInput = newPage + 1; // currentPage가 변경될 때 입력값 동기화
    },
  },
  computed: {
    rootMenus() {
      return this.getRootMenus();
    },
    isParentMenu() {
      if (!this.selectedMenu) return false;
      return this.menus.some(menu => menu.parentId === this.selectedMenu.id);
    },
  },
  methods: {
    async loadBlogAndMenus() {
      try {
        this.isLoading = true;
        const blogUrl = this.blogUrl || this.$route.params.blogUrl;
        const blog = await authService.getBlogByUrl(blogUrl);
        this.blog = blog;
        if (blog) {
          this.menus = await authService.getMenusByBlogId(blog.id);
          const routeMenuId = this.currentMenuId || this.$route.params.menuId;
          if (routeMenuId) {
            this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
          }
          this.currentUser = await authService.getCurrentUser();
          this.isOwner = blog.userId !== null && this.currentUser?.id === blog.userId;
        }
      } catch (error) {
        if (error.response?.status === 404) {
          this.$router.push('/404');
        } else if (error.response?.status === 403) {
          this.$router.push('/unauthorized');
        } else {
          this.blog = null;
        }
      } finally {
        this.isLoading = false;
      }
    },
    async loadPosts() {
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
        if (routeMenuId) {
          this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
        } else {
          this.selectedMenu = null;
        }
        this.posts.content = postsData.content || [];
        this.posts.totalPages = postsData.totalPages || 0;
      } catch (error) {
        console.error('Error loading posts:', error);
        this.posts.content = [];
        this.posts.totalPages = 0;
      }
    },
    searchPosts() {
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.selectedPosts = [];
      this.loadPosts();
    },
    changePage(newPage) {
      if (newPage >= 0 && newPage < this.posts.totalPages) {
        this.currentPage = newPage;
        this.selectedPosts = [];
        this.loadPosts();
      }
    },
    updatePage() {
      const newPage = this.currentPageInput - 1; // 입력값은 1부터 시작하므로 -1
      if (newPage >= 0 && newPage < this.posts.totalPages) {
        this.changePage(newPage);
      } else {
        this.currentPageInput = this.currentPage + 1; // 유효하지 않으면 원래 값으로 되돌림
        alert(`페이지 번호는 1에서 ${this.posts.totalPages} 사이여야 합니다.`);
      }
    },
    changePageSize() {
      this.currentPage = 0;
      this.currentPageInput = 1;
      this.selectedPosts = [];
      this.loadPosts();
    },
    toggleSelectAll() {
      if (this.selectedPosts.length === this.posts.content.length) {
        this.selectedPosts = [];
      } else {
        this.selectedPosts = this.posts.content.map(post => post.id);
      }
    },
    async bulkDeletePosts() {
      if (!this.isOwner) {
        alert('게시글을 삭제할 권한이 없습니다.');
        return;
      }
      if (this.selectedPosts.length === 0) {
        alert('삭제할 게시글을 선택해주세요.');
        return;
      }
      if (!confirm(`선택된 ${this.selectedPosts.length}개의 게시글을 정말 삭제하시겠습니까?`)) return;

      try {
        await Promise.all(
            this.selectedPosts.map(postId =>
                authService.deletePost(postId, this.currentUser.id)
            )
        );
        this.posts.content = this.posts.content.filter(post => !this.selectedPosts.includes(post.id));
        this.selectedPosts = [];
        alert('선택된 게시글이 삭제되었습니다.');
      } catch (error) {
        alert('게시글 삭제 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    formatDate(dateString) {
      if (!dateString) return '날짜 없음';
      const date = new Date(dateString);
      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();
      const isThisYear = date.getFullYear() === now.getFullYear();

      if (isToday) {
        return date.toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit', hour12: false});
      }
      const padZero = (num) => String(num).padStart(2, '0');
      const year = date.getFullYear();
      const month = padZero(date.getMonth() + 1);
      const day = padZero(date.getDate());

      if (isThisYear) return `${month}.${day}`;
      return `${year}.${month}.${day}`;
    },
    getRootMenus() {
      if (!Array.isArray(this.menus)) return [];
      return this.menus.filter(menu => !menu.parentId);
    },
    getSubMenus(parentId) {
      if (!Array.isArray(this.menus)) return [];
      return this.menus.filter(menu => menu.parentId === parentId);
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
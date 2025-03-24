<template>
  <div class="blog-view container mt-4">
    <NavBar />
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="blog" class="row">
      <div class="col-md-3">
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
      <div class="col-md-9">
        <h1>{{ blog.title }}</h1>
        <p>{{ blog.description }}</p>
        <div>
          <h2 v-if="selectedMenu">{{ selectedMenu.name }}</h2>
          <h2 v-else>전체 게시글</h2>
          <div v-if="isOwner && !isParentMenu" class="blog-actions mt-3">
            <router-link :to="`/${blog.url}/post/create${selectedMenu ? '?menuId=' + selectedMenu.id : ''}`"
                         class="btn btn-primary">글쓰기
            </router-link>
            <router-link :to="`/blog/edit/${blog.id}`" class="btn btn-secondary ml-2">블로그 수정</router-link>
          </div>
          <div class="post-list mt-4">
            <h3>게시글 목록</h3>
            <div v-if="!posts || posts.length === 0">게시글이 없습니다.</div>
            <ul v-else class="list-unstyled">
              <li v-for="post in posts" :key="post.id" class="post-item">
                <router-link :to="`/${blog.url}/${post.menuId}/${post.id}`">
                  {{ post.title }} [{{ post.commentCount || 0 }}]
                </router-link>
                <div v-if="isOwner" class="post-actions">
                  <router-link :to="`/post/edit/${post.id}`" class="btn btn-warning btn-sm mr-2">수정</router-link>
                  <button @click="deletePost(post.id)" class="btn btn-danger btn-sm">삭제</button>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      <h1>블로그를 찾을 수 없습니다.</h1>
      <router-link to="/" class="btn btn-primary">홈으로 돌아가기</router-link>
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
    const posts = reactive([]);
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
      currentUser: null, // 현재 사용자 정보 추가
    };
  },
  async created() {
    console.log('BlogView created, route params:', this.$route.params);
    console.log('Props - blogUrl:', this.blogUrl, 'menuId:', this.menuId);
    this.currentMenuId = this.menuId || this.$route.params.menuId;
    await this.loadBlogAndMenus();
    await this.loadPosts();
  },
  watch: {
    '$route.params.menuId'(newMenuId) {
      console.log('Route menuId changed to:', newMenuId);
      this.currentMenuId = newMenuId;
      this.loadPosts();
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
        console.log('Fetching blog with URL:', blogUrl);

        const blog = await authService.getBlogByUrl(blogUrl);
        console.log('Loaded blog:', blog);
        this.blog = blog;

        if (blog) {
          this.menus = await authService.getMenusByBlogId(blog.id);
          console.log('Loaded menus:', this.menus);

          const routeMenuId = this.currentMenuId || this.$route.params.menuId;
          if (routeMenuId) {
            this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
            console.log('Selected menu:', this.selectedMenu);
            if (!this.selectedMenu) {
              console.error('No menu found for menuId:', routeMenuId);
            }
          }

          // 현재 사용자 정보 가져오기
          this.currentUser = await authService.getCurrentUser();
          console.log('Current user from server:', this.currentUser);
          this.isOwner = blog.userId !== null && this.currentUser?.id === blog.userId;
          console.log('isOwner set to:', this.isOwner);
        }
      } catch (error) {
        console.error('블로그 또는 메뉴 로드 실패:', error.response?.data || error.message);
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
        let postsData;
        const routeMenuId = this.currentMenuId || this.$route.params.menuId;
        if (routeMenuId) {
          console.log('Loading posts for menuId:', routeMenuId);
          postsData = await authService.getPostsByMenuId(routeMenuId);
          console.log('Raw posts response (menu):', postsData);
          this.selectedMenu = this.menus.find(menu => menu.id === Number(routeMenuId));
        } else {
          console.log('Loading all posts for blogId:', this.blog.id);
          postsData = await authService.getAllPostsByBlogId(this.blog.id);
          console.log('Raw posts response (all):', postsData);
          this.selectedMenu = null;
        }
        this.posts.length = 0;
        if (Array.isArray(postsData)) {
          postsData.forEach(post => this.posts.push(post));
        }
        console.log('Assigned posts:', this.posts);
      } catch (error) {
        console.error('게시글 로드 실패:', error.response?.data || error.message);
        this.posts.length = 0;
      }
    },
    async deletePost(postId) {
      if (!this.isOwner) {
        alert('게시글을 삭제할 권한이 없습니다.');
        return;
      }
      if (!confirm('정말 삭제하시겠습니까?')) return;
      try {
        await authService.deletePost(postId, this.currentUser.id);
        const index = this.posts.findIndex(post => post.id === postId);
        if (index !== -1) this.posts.splice(index, 1);
        console.log('Post deleted:', postId);
      } catch (error) {
        console.error('게시글 삭제 실패:', error.response?.data || error.message);
        alert('게시글 삭제 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    getRootMenus() {
      if (!Array.isArray(this.menus)) {
        console.error('Menus is not an array:', this.menus);
        return [];
      }
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
  display: flex;
  flex-direction: column;
}

.menu-list {
  list-style: none;
  padding: 0;
}

.menu-item {
  margin-bottom: 10px;
}

.submenu-list {
  list-style: none;
  padding-left: 20px;
}

.submenu-item {
  margin-bottom: 5px;
}

.blog-actions {
  margin-top: 20px;
}

.post-list {
  margin-top: 30px;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ddd;
}

.post-actions {
  display: flex;
  gap: 10px;
}

.ml-2 {
  margin-left: 10px;
}

.mr-2 {
  margin-right: 10px;
}
</style>
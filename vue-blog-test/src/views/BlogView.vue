<template>
  <div class="blog-view container mt-4">
    <NavBar />
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="blog" class="row">
      <div class="col-md-3">
        <h3>메뉴</h3>
        <ul class="menu-list">
          <li v-for="menu in rootMenus" :key="menu.id" class="menu-item">
            <router-link :to="`/blog/${blog.url}/${menu.id}`">{{ menu.name }}</router-link>
            <ul v-if="getSubMenus(menu.id).length > 0" class="submenu-list">
              <li v-for="submenu in getSubMenus(menu.id)" :key="submenu.id" class="submenu-item">
                <router-link :to="`/blog/${blog.url}/${submenu.id}`">{{ submenu.name }}</router-link>
              </li>
            </ul>
          </li>
        </ul>
      </div>
      <div class="col-md-9">
        <h1>{{ blog.title }}</h1>
        <p>{{ blog.description }}</p>
        <div v-if="selectedMenu">
          <h2>{{ selectedMenu.name }}</h2>
          <div v-if="isOwner" class="blog-actions mt-3">
            <router-link :to="`/blog/${blog.url}/post/create`" class="btn btn-primary">글쓰기</router-link>
            <router-link :to="`/blog/edit/${blog.id}`" class="btn btn-secondary ml-2">블로그 수정</router-link>
          </div>
          <div class="post-list mt-4">
            <h3>게시글 목록</h3>
            <div v-if="posts.length === 0">게시글이 없습니다.</div>
            <div v-else>
              <div v-for="post in posts" :key="post.id" class="post-item">
                <h4>{{ post.title }}</h4>
                <p>{{ post.content }}</p>
                <div v-if="post.fileIds && post.fileIds.length > 0">
                  <p>첨부 파일: {{ post.fileIds.length }}개</p>
                </div>
                <div v-if="isOwner" class="post-actions">
                  <button @click="deletePost(post.id)" class="btn btn-danger btn-sm">삭제</button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else>
          <p>메뉴를 선택하세요.</p>
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
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'BlogView',
  components: { NavBar },
  props: {
    url: String,
    blogUrl: String,
    menuId: String,
  },
  data() {
    return {
      blog: null,
      menus: [],
      posts: [],
      isLoading: true,
      isOwner: false,
      selectedMenu: null,
    };
  },
  async created() {
    await this.loadBlogAndMenus();
  },
  watch: {
    menuId: {
      immediate: true,
      handler(newMenuId) {
        if (newMenuId && this.menus.length > 0) {
          this.loadPosts();
        }
      },
    },
  },
  methods: {
    async loadBlogAndMenus() {
      try {
        this.isLoading = true;
        const blogUrl = this.blogUrl || this.url || this.$route.params.url;
        console.log('Fetching blog with URL:', blogUrl);

        const blog = await authService.getBlogByUrl(blogUrl);
        console.log('Loaded blog:', blog);
        this.blog = blog;

        if (blog) {
          this.menus = await authService.getMenusByBlogId(blog.id);
          console.log('Loaded menus:', this.menus);

          if (this.menuId) {
            this.selectedMenu = this.menus.find(menu => menu.id === Number(this.menuId));
            console.log('Selected menu:', this.selectedMenu);
          }

          const storedUser = authService.getStoredUser();
          console.log('Stored user from localStorage:', storedUser);
          if (storedUser) {
            const accessToken = localStorage.getItem('accessToken');
            console.log('Access token:', accessToken ? 'present' : 'missing');
            try {
              console.log('Calling getCurrentUser with token:', accessToken);
              const user = await authService.getCurrentUser();
              console.log('Current user from server:', user);
              if (user && typeof user === 'object' && 'id' in user) {
                this.isOwner = blog.userId !== null && user.id === blog.userId;
                console.log('isOwner set to:', this.isOwner);
              } else {
                console.log('User object invalid or missing id:', user);
                this.isOwner = false;
              }
            } catch (error) {
              console.error('사용자 정보 로드 실패:', error);
              this.isOwner = false;
              if (error.message === 'No access token available' || (error.response && error.response.status === 401)) {
                console.log('토큰 문제로 로그인 페이지로 리다이렉트');
                this.$router.push('/login');
              }
            }
          } else {
            console.log('저장된 사용자가 없음');
            this.isOwner = false;
          }
        }
      } catch (error) {
        console.error('블로그 또는 메뉴 로드 실패:', error);
        this.blog = null;
      } finally {
        this.isLoading = false;
      }
    },
    async loadPosts() {
      if (!this.menuId) return;
      try {
        this.posts = await authService.getPostsByMenuId(this.menuId);
        console.log('Loaded posts:', this.posts);
      } catch (error) {
        console.error('게시글 로드 실패:', error);
        this.posts = [];
      }
    },
    async deletePost(postId) {
      if (!confirm('정말 삭제하시겠습니까?')) return;
      try {
        const user = await authService.getCurrentUser();
        await authService.deletePost(postId, user.id);
        this.posts = this.posts.filter(post => post.id !== postId);
        console.log('Post deleted:', postId);
      } catch (error) {
        console.error('게시글 삭제 실패:', error);
        alert('게시글 삭제 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    getRootMenus() {
      return this.menus.filter(menu => !menu.parentId);
    },
    getSubMenus(parentId) {
      return this.menus.filter(menu => menu.parentId === parentId);
    },
  },
  computed: {
    rootMenus() {
      return this.getRootMenus();
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
  border: 1px solid #ddd;
  padding: 15px;
  margin-bottom: 15px;
  border-radius: 5px;
}

.post-actions {
  margin-top: 10px;
}

.ml-2 {
  margin-left: 10px;
}
</style>
<template>
  <div class="home">
    <NavBar />
    <div class="container mt-4">
      <div class="row">
        <div class="col-md-8">
          <h1>환영합니다!</h1>
          <p>블로그 플랫폼에 오신 것을 환영합니다.</p>
          <div class="popular-posts">
            <h3>인기글</h3>
            <p>추후 구현 예정...</p>
          </div>
        </div>
        <div class="col-md-4">
          <div class="blog-sidebar">
            <h3>내 블로그</h3>
            <div v-if="isLoading">로딩 중...</div>
            <div v-else-if="!isLoggedIn">
              <p>블로그를 생성하려면 로그인이 필요합니다.</p>
              <router-link to="/login" class="btn btn-primary">로그인</router-link>
            </div>
            <div v-else-if="!blog">
              <p>아직 블로그가 없습니다.</p>
              <router-link to="/blog/create" class="btn btn-primary">블로그 생성하기</router-link>
            </div>
            <div v-else>
              <p>{{ blog.title }}</p>
              <router-link :to="`/blog/${blog.url}`" class="btn btn-success">내 블로그 가기</router-link>
              <router-link to="/blog/edit" class="btn btn-secondary ms-2">블로그 수정</router-link>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'HomeView',
  components: { NavBar },
  data() {
    return {
      blog: null,
      isLoading: true,
      isLoggedIn: false,
    };
  },
  async created() {
    await this.loadBlog();
  },
  methods: {
    async loadBlog() {
      try {
        this.isLoading = true;
        const storedUser = authService.getStoredUser();
        this.isLoggedIn = !!storedUser;
        if (storedUser) {
          const userData = await authService.getCurrentUser();
          const blogData = await authService.getBlogByUserId(userData.id);
          this.blog = blogData; // null이면 블로그 없음
        } else {
          this.blog = null;
        }
      } catch (error) {
        console.error('블로그 로드 실패:', error);
        this.blog = null;
        this.isLoggedIn = false;
        if (error.response?.status === 401) {
          this.$router.push('/login');
        }
      } finally {
        this.isLoading = false;
      }
    },
  },
};
</script>

<style scoped>
.blog-sidebar {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.btn {
  padding: 8px 16px;
  font-size: 14px;
}
</style>
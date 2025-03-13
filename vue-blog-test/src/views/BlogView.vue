<template>
  <div class="blog-view container mt-4">
    <NavBar />
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="blog">
      <h1>{{ blog.title }}</h1>
      <p>{{ blog.description }}</p>
      <div v-if="isOwner" class="blog-actions">
        <router-link to="/blog/edit" class="btn btn-secondary">블로그 수정</router-link>
      </div>
      <div class="blog-content">
        <h3>게시글</h3>
        <p>게시글 목록은 추후 구현 예정...</p>
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
  data() {
    return {
      blog: null,
      isLoading: true,
      isOwner: false,
    };
  },
  async created() {
    await this.loadBlog();
  },
  methods: {
    async loadBlog() {
      try {
        this.isLoading = true;
        const url = this.$route.params.url;
        console.log('Fetching blog with URL:', url);
        const blog = await authService.getBlogByUrl(url);
        console.log('Loaded blog:', blog);
        this.blog = blog;

        if (blog) {
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
                // blog.user 대신 blog.userId 사용
                this.isOwner = blog.userId !== null && user.id === blog.userId;
                console.log('isOwner set to:', this.isOwner);
              } else {
                console.log('User object invalid or missing id:', user);
                this.isOwner = false;
              }
            } catch (error) {
              console.error('사용자 정보 로드 실패:', error);
              console.log('Error details:', error.response ? error.response.data : error.message);
              console.log('Error status:', error.response ? error.response.status : 'N/A');
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
        console.error('블로그 로드 실패:', error);
        this.blog = null;
      } finally {
        this.isLoading = false;
      }
    },
  },
};
</script>

<style scoped>
.blog-actions {
  margin-top: 20px;
}
.blog-content {
  margin-top: 30px;
}
</style>
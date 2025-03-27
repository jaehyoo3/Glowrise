<template>
  <div class="home">
    <NavBar />
    <div class="container">
      <div class="home-content">
        <div class="hero-section">
          <h1 class="site-title">Glowrise</h1>
          <p class="site-description">Create, Share, Inspire</p>
        </div>

        <div class="main-grid">
          <div class="blog-overview">
            <h2>Welcome to Glowrise</h2>
            <p>Your journey of sharing stories begins here.</p>

            <div class="popular-posts">
              <h3>Featured Posts</h3>
              <div class="placeholder-posts">
                <p>Trending content coming soon...</p>
              </div>
            </div>
          </div>

          <div class="blog-sidebar">
            <div v-if="isLoading" class="loading-state">
              <span>Loading...</span>
            </div>

            <div v-else-if="!isLoggedIn" class="login-prompt">
              <h3>Start Your Blog</h3>
              <p>Sign in to create your personal space</p>
              <router-link to="/login" class="btn-primary">Sign In</router-link>
            </div>

            <div v-else-if="!blog" class="create-blog-prompt">
              <h3>Your Blog</h3>
              <p>You haven't created a blog yet</p>
              <router-link to="/blog/create" class="btn-secondary">Create Blog</router-link>
            </div>

            <div v-else class="blog-info">
              <h3>{{ blog.title }}</h3>
              <div class="blog-actions">
                <router-link :to="`/${blog.url}`" class="btn-primary">View Blog</router-link>
                <router-link :to="`/blog/edit/${blog.id}`" class="btn-secondary">Edit</router-link>
              </div>
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
          this.blog = blogData;
        } else {
          this.blog = null;
        }
      } catch (error) {
        console.error('Blog load failed:', error);
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
.home {
  background-color: #f8f9fa;
  min-height: 100vh;
  color: #333;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.hero-section {
  text-align: center;
  padding: 3rem 0;
}

.site-title {
  font-size: 3.5rem;
  font-weight: 700;
  color: #000;
  margin-bottom: 0.5rem;
}

.site-description {
  font-size: 1.2rem;
  color: #666;
}

.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

.blog-overview, .blog-sidebar {
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.blog-sidebar h3 {
  border-bottom: 2px solid #000;
  padding-bottom: 0.5rem;
  margin-bottom: 1rem;
}

.btn-primary, .btn-secondary {
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

.btn-secondary {
  background-color: #f8f9fa;
  color: #000;
  border: 1px solid #000;
  margin-left: 0.5rem;
}

.blog-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.placeholder-posts {
  background-color: #f8f9fa;
  padding: 1rem;
  border-radius: 4px;
  text-align: center;
}

@media (max-width: 768px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}
</style>
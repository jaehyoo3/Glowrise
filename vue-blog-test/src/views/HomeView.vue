<template>
  <div class="home">
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
              <div v-if="isPostsLoading" class="loading-state">
                <span>Loading popular posts...</span>
              </div>
              <div v-else-if="!popularPosts || popularPosts.length === 0" class="placeholder-posts">
                <p>Trending content coming soon...</p>
              </div>
              <div v-else class="posts-grid">
                <div
                    v-for="post in popularPosts"
                    :key="post.id"
                    class="post-card"
                    @click="navigateToPost(post)">
                  <div class="post-content">
                    <h4 class="post-title">{{ post.title }}</h4>
                    <p class="post-excerpt">{{ truncateContent(post.content) }}</p>
                    <div class="post-meta">
                      <span><i class="fa-regular fa-eye"></i> {{ post.viewCount || 0 }}</span>
                      <span><i class="fa-regular fa-comment"></i> {{ post.commentCount || 0 }}</span>
                      <span><i class="fa-regular fa-clock"></i> {{ formatDate(post.updatedAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="time-period-filter">
                <button
                    :class="{ active: selectedPeriod === 'DAILY' }"
                    class="period-btn"
                    @click="changeTimePeriod('DAILY')"
                >
                  Daily
                </button>
                <button
                    :class="{ active: selectedPeriod === 'WEEKLY' }"
                    class="period-btn"
                    @click="changeTimePeriod('WEEKLY')"
                >
                  Weekly
                </button>
                <button
                    :class="{ active: selectedPeriod === 'MONTHLY' }"
                    class="period-btn"
                    @click="changeTimePeriod('MONTHLY')"
                >
                  Monthly
                </button>
              </div>
            </div>
          </div>

          <div class="blog-sidebar">
            <div v-if="isSidebarLoading" class="loading-state">
              <span>Loading...</span>
            </div>
            <div v-else-if="!isLoggedIn" class="login-prompt">
              <h3>Start Your Blog</h3>
              <p>Sign in or sign up to create your personal space and share your stories.</p>
              <p style="margin-top: 1rem; font-size: 0.9em; color: #555;">Please use the login/signup buttons in the
                navigation bar.</p>
            </div>
            <div v-else-if="isLoggedIn && !hasBlog" class="create-blog-prompt">
              <h3>Your Blog Awaits</h3>
              <template v-if="nickName">
                <p>You haven't created a blog yet. Let's get started!</p>
                <router-link class="btn-secondary" to="/blog/create">Create Your Blog</router-link>
              </template>
              <template v-else>
                <p>Welcome! Please set your nickname first to create a blog.</p>
                <p style="font-size: 0.85em; color: #666;">(You can set it from the user menu in the navigation bar)</p>
              </template>
            </div>
            <div v-else-if="isLoggedIn && hasBlog" class="blog-info">
              <h3>My Blog: {{ blogTitle }}</h3>
              <p>Manage your blog or view it live.</p>
              <div class="blog-actions">
                <router-link :to="`/${blogUrl}`" class="btn-primary">View Blog</router-link>
                <router-link v-if="blogId" :to="`/blog/edit/${blogId}`" class="btn-secondary">Manage Blog</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // 인기글 로딩 등 직접 API 호출용

export default {
  name: 'HomeView',
  // NavBar는 App.vue 등 상위에서 관리한다고 가정
  // components: { NavBar },
  data() {
    return {
      // --- 로컬 상태 제거: blog, isLoggedIn, userNickname ---
      // --- isLoading은 스토어 getter(isLoading) 또는 컴포넌트 자체 로딩(isPostsLoading)으로 대체 ---
      popularPosts: [],        // 인기글 목록 (로컬 상태 유지)
      isPostsLoading: true,    // 인기글 로딩 상태 (로컬 상태 유지)
      selectedPeriod: 'WEEKLY',// 인기글 기간 (로컬 상태 유지)
    };
  },
  computed: {
    // --- Vuex Getters/State 매핑 ---
    ...mapGetters([
      'isLoggedIn',       // 로그인 여부
      'nickName',         // 닉네임 (표시용)
      'username',         // 사용자 이름 (닉네임 없을 때 대비)
      'hasBlog',          // 블로그 소유 여부
      'blogUrl',          // 사용자 블로그 URL
      'isLoadingUser',    // 스토어의 사용자 정보 로딩 상태
      'isLoadingBlog',     // 스토어의 블로그 정보 로딩 상태
      'blogId',           // 사용자 블로그 ID getter
      'blogTitle',        // 사용자 블로그 제목 getter
    ]),
    // 스토어 상태 직접 매핑 (필요한 경우)
    // ...mapState(['currentUser', 'userBlog']),

    // 닉네임 또는 사용자 이름 표시
    displayNicknameOrUsername() {
      return this.nickName || this.username;
    },
    // 사이드바 로딩 상태 (스토어 로딩 상태 조합)
    isSidebarLoading() {
      return this.isLoadingUser || this.isLoadingBlog;
    }
    // ---------------------------
  },
  // --- 삭제: created 내부의 loadBlogAndCheckLogin 호출 ---
  // --- 삭제: mounted, beforeUnmount, handleAuthChange (이벤트 리스너 관련) ---
  // --- mounted 또는 created 에서 인기글 로딩 시작 ---
  async mounted() {
    console.log("HomeView: mounted hook. 인기글 로드 시작.");
    await this.loadPopularPosts();
  },
  methods: {
    // --- 삭제: loadBlogAndCheckLogin 메서드 ---

    // 인기글 로딩 메서드 (기존과 동일, authService 직접 사용)
    async loadPopularPosts() {
      console.log(`HomeView: 인기글 로드 중 (${this.selectedPeriod})`);
      this.isPostsLoading = true;
      this.popularPosts = [];
      try {
        const popularData = await authService.getPopularPosts(this.selectedPeriod);
        if (Array.isArray(popularData)) {
          this.popularPosts = popularData;
          console.log(`HomeView: 인기글 로드 완료 (${this.popularPosts.length}개)`);
        } else {
          console.warn("HomeView: getPopularPosts 응답이 배열이 아님:", popularData);
          this.popularPosts = [];
        }
      } catch (error) {
        console.error('HomeView: 인기글 로드 실패:', error);
        this.popularPosts = [];
      } finally {
        this.isPostsLoading = false;
      }
    },

    // --- 나머지 메서드 (truncateContent, formatDate, navigateToPost, changeTimePeriod)는 기존과 동일 ---
    truncateContent(content) {
      if (!content) return '';
      const textContent = content.replace(/<[^>]*>/g, '');
      const maxLength = 80;
      if (textContent.length > maxLength) {
        return textContent.substring(0, maxLength) + '...';
      }
      return textContent;
    },
    formatDate(dateString) {
      if (!dateString) return '';
      try {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {year: 'numeric', month: '2-digit', day: '2-digit'}).replace(/\.$/, '');
      } catch (e) {
        return dateString;
      }
    },
    navigateToPost(post) {
      if (post && post.blogUrl && post.menuId && post.id) {
        const path = `/${post.blogUrl}/${post.menuId}/${post.id}`;
        this.$router.push(path).catch({ /* ... */});
      } else {
        console.warn("HomeView: 포스트 네비게이션 정보 부족.", post);
      }
    },
    async changeTimePeriod(period) {
      if (this.selectedPeriod === period || this.isPostsLoading) return;
      console.log(`HomeView: 인기글 기간 변경 -> ${period}`);
      this.selectedPeriod = period;
      await this.loadPopularPosts();
    }
  }
};
</script>
<style scoped>
/* --- 기본 레이아웃 및 컨테이너 --- */
.home {
  background-color: #fafafa; /* 매우 연한 그레이 배경 */
  min-height: 100vh;
  color: #212121; /* 진한 검정 텍스트 */
  font-family: 'Inter', 'Helvetica Neue', sans-serif; /* 모던한 산세리프 폰트 */
}

.container {
  max-width: 1140px;
  margin: 0 auto;
  padding: 2rem;
  box-sizing: border-box;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 3rem; /* 섹션 간 간격 증가 */
}

/* --- 히어로 섹션 --- */
.hero-section {
  text-align: center;
  padding: 4rem 0; /* 패딩 증가 */
}

.site-title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  font-weight: 800; /* 더 굵게 */
  color: #000; /* 순수 검정 */
  margin-bottom: 0.5rem;
  letter-spacing: -0.02em; /* 레터스페이싱 */
}

.site-description {
  font-size: clamp(1rem, 3vw, 1.2rem);
  color: #757575; /* 미디엄 그레이 */
  font-weight: 300; /* 가벼운 폰트 웨이트 */
  letter-spacing: 0.05em; /* 약간 넓은 레터스페이싱 */
}

/* --- 메인 그리드 --- */
.main-grid {
  display: grid;
  /* 기본 2열, 작은 화면에서는 1열 */
  grid-template-columns: repeat(auto-fit, minmax(min(300px, 100%), 1fr));
  gap: 2.5rem; /* 그리드 아이템 간 간격 증가 */
}

.blog-overview, .blog-sidebar {
  background-color: white;
  border-radius: 4px; /* 더 작은 둥글기 */
  padding: 2.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); /* 매우 미묘한 그림자 */
}

.blog-sidebar h3, .popular-posts h3, .blog-overview h2 {
  border-bottom: 1px solid #eee; /* 더 얇은 구분선 */
  padding-bottom: 0.8rem;
  margin-top: 0; /* 제목 위 기본 마진 제거 */
  margin-bottom: 1.8rem;
  font-size: 1.4rem;
  color: #000;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.blog-overview h2 {
  font-size: 1.6rem;
}

/* Welcome 메시지 약간 더 크게 */


/* --- 버튼 스타일 --- */
.btn-primary, .btn-secondary {
  display: inline-block;
  padding: 0.7rem 1.6rem;
  border-radius: 3px; /* 더 작은 둥글기 */
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
  text-align: center;
  cursor: pointer;
  font-size: 0.9rem; /* 버튼 폰트 크기 약간 줄임 */
  letter-spacing: 0.02em;
}

.btn-primary {
  background-color: #000; /* 검정색 버튼 */
  color: white;
  border: 1px solid #000;
}

.btn-primary:hover {
  background-color: #333; /* 호버 시 약간 밝은 검정 */
  border-color: #333;
}

.btn-secondary {
  background-color: white;
  color: #000;
  border: 1px solid #ddd;
  /* margin-left 제거 (gap으로 처리) */
}

.btn-secondary:hover {
  background-color: #f5f5f5;
  border-color: #ccc;
}

.blog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem; /* 버튼 간 간격 */
  margin-top: 1.2rem;
}

.login-prompt p, .create-blog-prompt p { /* 사이드바 텍스트 스타일 */
  color: #555;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

/* --- 플레이스홀더 및 로딩 상태 --- */
.placeholder-posts, .loading-state {
  background-color: #f5f5f5;
  padding: 2rem;
  border-radius: 3px;
  text-align: center;
  color: #757575;
  min-height: 120px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* --- 인기 게시글 카드 --- */
.posts-grid {
  display: grid;
  /* 화면 크기에 따라 1개 또는 2개 표시 */
  grid-template-columns: repeat(auto-fit, minmax(min(250px, 100%), 1fr));
  gap: 1.5rem; /* 카드 간 간격 */
  margin-top: 1.2rem;
}

.post-card {
  border: none; /* 테두리 제거 */
  border-radius: 3px;
  overflow: hidden;
  background: white;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06); /* 그림자 약간 조정 */
}

.post-card:hover {
  transform: translateY(-4px); /* 호버 시 약간 위로 이동 */
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.post-content {
  padding: 1.5rem;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.post-title {
  margin: 0 0 0.8rem 0;
  font-size: 1.1rem; /* 제목 크기 */
  font-weight: 600;
  line-height: 1.4;
  color: #111; /* 제목 색상 약간 조정 */
  letter-spacing: -0.01em;
  /* 여러 줄 말줄임 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-height: 3.08em; /* line-height * 2줄 */
}

.post-excerpt {
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 1.2rem;
  line-height: 1.6;
  flex-grow: 1;
  /* 여러 줄 말줄임 */
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  max-height: 4.32em; /* line-height * 3줄 */
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  font-size: 0.75rem; /* 메타 폰트 크기 약간 줄임 */
  color: #9e9e9e;
  gap: 0.8rem;
  margin-top: auto;
  padding-top: 0.7rem;
  border-top: 1px solid #f0f0f0;
}

.post-meta span {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}
.post-meta span i {
  font-size: 0.9em;
  position: relative;
  top: -1px; /* 아이콘 미세 조정 */
}

/* --- 기간 필터 버튼 --- */
.time-period-filter {
  display: flex;
  justify-content: flex-end; /* 오른쪽 정렬 */
  gap: 0.5rem;
  margin-top: 1.8rem;
}

.period-btn {
  border: 1px solid #e0e0e0;
  background: white;
  padding: 0.4rem 1rem;
  border-radius: 20px;
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #757575;
  font-weight: 500;
}

.period-btn:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
}

.period-btn.active {
  background: #000; /* 검정색 활성 버튼 */
  color: white;
  border-color: #000;
}

/* --- 모달 관련 스타일 제거됨 --- */

/* --- 반응형 조정 --- */
@media (max-width: 992px) {
  .main-grid {
    grid-template-columns: 3fr 2fr;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 1.5rem;
  }

  .hero-section {
    padding: 3rem 0;
  }
  .main-grid {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .blog-overview, .blog-sidebar {
    padding: 1.8rem;
  }

  .blog-overview {
    order: 1;
  }

  .blog-sidebar {
    order: 2;
  }

  .posts-grid {
    grid-template-columns: 1fr;
  }

  .time-period-filter {
    justify-content: center;
  }
}
</style>
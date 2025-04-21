<template>
  <div class="home">
    <div class="container">
      <!-- 광고 섹션 -->
      <main-advertisement class="ad-section"/>

      <div class="main-content">
        <!-- 인기 게시글 섹션 -->
        <section class="popular-posts-section">
          <div class="section-header">
            <h2>인기 게시글</h2>
            <div class="time-period-filter">
              <button
                  :class="{ active: selectedPeriod === 'DAILY' }"
                  class="period-btn"
                  @click="changeTimePeriod('DAILY')"
              >
                일간
              </button>
              <button
                  :class="{ active: selectedPeriod === 'WEEKLY' }"
                  class="period-btn"
                  @click="changeTimePeriod('WEEKLY')"
              >
                주간
              </button>
              <button
                  :class="{ active: selectedPeriod === 'MONTHLY' }"
                  class="period-btn"
                  @click="changeTimePeriod('MONTHLY')"
              >
                월간
              </button>
            </div>
          </div>

          <div v-if="isPostsLoading" class="loading-state">
            <span>인기 게시글을 불러오는 중...</span>
          </div>
          <div v-else-if="!popularPosts || popularPosts.length === 0" class="placeholder-posts">
            <p>곧 인기 콘텐츠가 업데이트될 예정입니다.</p>
          </div>
          <div v-else class="posts-grid">
            <div
                v-for="post in popularPosts"
                :key="post.id"
                class="post-card"
                @click="navigateToPost(post)">
              <div class="post-content">
                <h3 class="post-title">{{ post.title }}</h3>
                <p class="post-excerpt">{{ truncateContent(post.content) }}</p>
                <div class="post-meta">
                  <span><i class="fa-regular fa-eye"></i> {{ post.viewCount || 0 }}</span>
                  <span><i class="fa-regular fa-comment"></i> {{ post.commentCount || 0 }}</span>
                  <span><i class="fa-regular fa-clock"></i> {{ formatDate(post.updatedAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 사이드바 섹션 -->
        <section class="sidebar-section">
          <div v-if="isSidebarLoading" class="loading-state">
            <span>로딩 중...</span>
          </div>
          <div v-else-if="!isLoggedIn" class="auth-prompt">
            <h3>블로그 시작하기</h3>
            <p>로그인하거나 회원가입하여 나만의 블로그를 만들어보세요.</p>
            <div class="action-hint">상단 네비게이션 바의 로그인/회원가입 버튼을 이용해주세요.</div>
          </div>
          <div v-else-if="isLoggedIn && !hasBlog" class="create-blog-prompt">
            <h3>블로그 만들기</h3>
            <template v-if="nickName">
              <p>아직 블로그를 만들지 않았습니다. 지금 시작해보세요!</p>
              <router-link class="btn-primary" to="/blog/create">블로그 만들기</router-link>
            </template>
            <template v-else>
              <p>환영합니다! 블로그를 만들기 전에 먼저 닉네임을 설정해주세요.</p>
              <div class="action-hint">(상단 네비게이션 바의 사용자 메뉴에서 설정 가능합니다)</div>
            </template>
          </div>
          <div v-else-if="isLoggedIn && hasBlog" class="blog-info">
            <h3>내 블로그: {{ blogTitle }}</h3>
            <p>블로그를 관리하거나 방문해보세요.</p>
            <div class="blog-actions">
              <router-link :to="`/${blogUrl}`" class="btn-primary">블로그 방문</router-link>
              <router-link v-if="blogId" :to="`/blog/edit/${blogId}`" class="btn-secondary">블로그 관리</router-link>
            </div>
          </div>

          <!-- 환영 메시지 -->
          <div class="welcome-block">
            <h3>Glowrise에 오신 것을 환영합니다</h3>
            <p>여러분의 이야기를 공유하는 여정이 여기서 시작됩니다. 매일 의미 있는 콘텐츠를 만들어내는 작가와 독자들의 커뮤니티에 참여해보세요.</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
import {mapGetters} from 'vuex';
import authService from '@/services/authService';
import MainAdvertisement from '@/components/MainAdvertisement.vue';

export default {
  name: 'HomeView',
  components: {
    MainAdvertisement
  },
  data() {
    return {
      popularPosts: [],
      isPostsLoading: true,
      selectedPeriod: 'WEEKLY',
    };
  },
  computed: {
    ...mapGetters([
      'isLoggedIn',
      'nickName',
      'username',
      'hasBlog',
      'blogUrl',
      'isLoadingUser',
      'isLoadingBlog',
      'blogId',
      'blogTitle',
    ]),
    displayNicknameOrUsername() {
      return this.nickName || this.username;
    },
    isSidebarLoading() {
      return this.isLoadingUser || this.isLoadingBlog;
    }
  },
  async mounted() {
    console.log("HomeView: mounted hook. 인기글 로드 시작.");
    await this.loadPopularPosts();
  },
  methods: {
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
    truncateContent(content) {
      if (!content) return '';
      const textContent = content.replace(/<[^>]*>/g, '');
      const maxLength = 100;
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
        this.$router.push(path).catch({ /* navigation duplicate handling */});
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
/* --- 기본 레이아웃 --- */
.home {
  background-color: #f8f8f8;
  min-height: 100vh;
  color: #333;
  font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

/* --- 광고 섹션 --- */
.ad-section {
  margin-bottom: 2rem;
  width: 100%;
}

/* --- 메인 콘텐츠 레이아웃 --- */
.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
}

/* --- 인기 게시글 섹션 --- */
.popular-posts-section {
  background-color: white;
  padding: 2rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.8rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.section-header h2 {
  font-size: 1.4rem;
  font-weight: 600;
  margin: 0;
  color: #222;
  letter-spacing: -0.01em;
}

/* 게시글 그리드 */
.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.5rem;
}

.post-card {
  background-color: white;
  border: 1px solid #eaeaea;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 100%;
}

.post-card:hover {
  border-color: #d0d0d0;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.06);
}

.post-content {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.post-title {
  font-size: 1.1rem;
  margin: 0 0 1rem 0;
  line-height: 1.4;
  font-weight: 600;
  color: #222;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-excerpt {
  color: #666;
  line-height: 1.6;
  flex-grow: 1;
  font-size: 0.95rem;
  margin-bottom: 1.2rem;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  font-size: 0.8rem;
  color: #888;
  gap: 1rem;
  border-top: 1px solid #f0f0f0;
  padding-top: 0.8rem;
}

.post-meta span {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

/* 필터 버튼 */
.time-period-filter {
  display: flex;
  gap: 0.5rem;
}

.period-btn {
  border: 1px solid #e0e0e0;
  background: white;
  padding: 0.4rem 1rem;
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #666;
}

.period-btn:hover {
  background: #f5f5f5;
}

.period-btn.active {
  background: #333;
  color: white;
  border-color: #333;
}

/* --- 사이드바 섹션 --- */
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.sidebar-section > div {
  background-color: white;
  padding: 1.8rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.sidebar-section h3 {
  font-size: 1.2rem;
  font-weight: 600;
  margin-top: 0;
  margin-bottom: 1rem;
  color: #222;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid #eee;
}

.action-hint {
  margin-top: 0.8rem;
  font-size: 0.85rem;
  color: #777;
}

/* 환영 블록 */
.welcome-block {
  background-color: white;
}

.welcome-block p {
  color: #666;
  line-height: 1.6;
}

/* 버튼 스타일 */
.btn-primary, .btn-secondary {
  display: inline-block;
  padding: 0.7rem 1.4rem;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
  text-align: center;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-primary {
  background-color: #333;
  color: white;
  border: 1px solid #333;
}

.btn-primary:hover {
  background-color: #222;
}

.btn-secondary {
  background-color: white;
  color: #333;
  border: 1px solid #ddd;
}

.btn-secondary:hover {
  background-color: #f5f5f5;
  border-color: #ccc;
}

.blog-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 1.2rem;
}

/* 로딩 및 플레이스홀더 상태 */
.loading-state, .placeholder-posts {
  background-color: #f9f9f9;
  padding: 2rem;
  text-align: center;
  color: #777;
  min-height: 120px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 미디어 쿼리 */
@media (max-width: 992px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .sidebar-section {
    order: 1;
    margin-bottom: 1.5rem;
  }

  .popular-posts-section {
    order: 2;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 1.5rem 1rem;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .time-period-filter {
    width: 100%;
    justify-content: flex-start;
  }

  .period-btn {
    flex: 1;
    text-align: center;
  }

  .posts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
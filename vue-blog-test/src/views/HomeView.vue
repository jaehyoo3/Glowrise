<template>
  <div class="home">
    <div class="container">
      <section class="hero-section">
        <swiper
            :autoplay="{
            delay: 3000,
            disableOnInteraction: false,
          }"
            :loop="true"
            :modules="swiperModules"
            :pagination="{ clickable: true }"
            :slides-per-view="1"
            :space-between="0"
            class="hero-swiper"
        >
          <swiper-slide v-for="slide in heroSlides" :key="slide.id" class="hero-slide">
            <router-link :to="slide.link" class="hero-slide-link">
              <img :alt="slide.title" :src="slide.imageUrl" class="hero-image">
              <div class="hero-text-content">
                <h2>{{ slide.title }}</h2>
                <p>{{ slide.description }}</p>
              </div>
            </router-link>
          </swiper-slide>
        </swiper>
      </section>

      <main-advertisement class="ad-section"/>

      <div class="main-content">
        <section id="popular-posts-section" class="popular-posts-section">
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

import {Swiper, SwiperSlide} from 'swiper/vue';
import {Autoplay, Pagination} from 'swiper/modules';

import 'swiper/css';
import 'swiper/css/pagination';

export default {
  name: 'HomeView',
  components: {
    MainAdvertisement,
    Swiper,
    SwiperSlide,
  },
  data() {
    return {
      popularPosts: [],
      isPostsLoading: true,
      selectedPeriod: 'WEEKLY',
      heroSlides: [
        {
          id: 1,
          title: 'Glowrise에 오신 것을 환영합니다!',
          description: '당신의 이야기를 세상과 공유하세요.',
          imageUrl: 'https://via.placeholder.com/800x250/FFA07A/ffffff?text=Slide+1+%28Replace+Me%29',
          link: '/about'
        },
        {
          id: 2,
          title: '인기 글 살펴보기',
          description: '지금 가장 주목받는 글들을 만나보세요.',
          imageUrl: 'https://via.placeholder.com/800x250/20B2AA/ffffff?text=Slide+2+%28Replace+Me%29',
          link: '#popular-posts-section'
        },
        {
          id: 3,
          title: '나만의 블로그 시작하기',
          description: '쉽고 빠르게 당신의 공간을 만들어보세요.',
          imageUrl: 'https://via.placeholder.com/800x250/778899/ffffff?text=Slide+3+%28Replace+Me%29',
          link: '/blog/create'
        },
        {
          id: 4,
          title: '최신 업데이트 소식',
          description: 'Glowrise의 새로운 기능들을 확인해보세요.',
          imageUrl: 'https://via.placeholder.com/800x250/6A5ACD/ffffff?text=Slide+4+%28Replace+Me%29',
          link: '/news'
        },
        {
          id: 5,
          title: '다양한 주제 탐색',
          description: '여행, 기술, 일상 등 흥미로운 글들이 가득합니다.',
          imageUrl: 'https://via.placeholder.com/800x250/FF6347/ffffff?text=Slide+5+%28Replace+Me%29',
          link: '/explore'
        }
      ],
      swiperModules: [Autoplay, Pagination],
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
        console.error("날짜 형식 변환 오류:", e);
        return dateString;
      }
    },
    navigateToPost(post) {
      if (post && post.blogUrl && post.menuId && post.id) {
        const path = `/${post.blogUrl}/${post.menuId}/${post.id}`;
        this.$router.push(path).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            console.error('네비게이션 오류:', err);
          }
        });
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
.home {
  background-color: #f8f8f8;
  min-height: 100vh;
  color: #333;
  font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem; /* 컨테이너 패딩 유지 (히어로 섹션이 컨테이너 내부에 위치) */
  display: block;
}

/* 히어로 섹션 스타일 수정 */
.hero-section {
  background-color: #f0f0f0;
  overflow: hidden;
  position: relative;
  height: 450px;
}

.hero-swiper {
  width: 100%;
  height: 100%;
}

.hero-slide {
  position: relative;
  height: 100%;
  background-color: #e0e0e0;
}

.hero-slide-link {
  display: block;
  width: 100%;
  height: 100%;
  text-decoration: none;
  color: inherit;
}

.hero-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.hero-text-content {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 1.5rem 2rem;
  box-sizing: border-box;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7) 0%, rgba(0, 0, 0, 0) 100%);
  color: white;
  pointer-events: none;
}

.hero-text-content h2 {
  margin: 0 0 0.5rem 0;
  font-size: 1.6rem;
  font-weight: 600;
  text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.5);
}

.hero-text-content p {
  margin: 0;
  font-size: 1rem;
  opacity: 0.9;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
}

.hero-swiper .swiper-pagination {
  bottom: 10px !important;
}

.hero-swiper .swiper-pagination-bullet {
  background-color: rgba(255, 255, 255, 0.6);
  opacity: 1;
  transition: background-color 0.2s;
}

.hero-swiper .swiper-pagination-bullet-active {
  background-color: #ffffff;
}

/* 광고 섹션 스타일 수정 */
.ad-section {
  margin-top: 2rem; /* 히어로 섹션과의 간격 */
  margin-bottom: 2rem;
  width: 100%;
}

/* 메인 콘텐츠 영역 */
.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 2rem;
  /* 상단 마진은 ad-section이 담당 */
}

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
  display: flex;
  flex-direction: column;
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
  text-overflow: ellipsis;
  min-height: calc(1.1rem * 1.4 * 2);
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
  text-overflow: ellipsis;
}

.post-meta {
  margin-top: auto;
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

.sidebar-section p {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #555;
  margin-bottom: 1rem;
}

.action-hint {
  margin-top: 0.8rem;
  font-size: 0.85rem;
  color: #777;
}

.welcome-block {
  background-color: white;
}

.welcome-block p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 0;
}

.btn-primary, .btn-secondary {
  display: inline-block;
  padding: 0.7rem 1.4rem;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
  text-align: center;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 4px; /* 버튼 모서리는 둥글게 유지 */
}

.btn-primary {
  background-color: #333;
  color: white;
  border: 1px solid #333;
}

.btn-primary:hover {
  background-color: #222;
  border-color: #222;
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

.loading-state, .placeholder-posts {
  background-color: #f9f9f9;
  padding: 2rem;
  text-align: center;
  color: #777;
  min-height: 120px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #eee;
}

/* 반응형 스타일 */
@media (max-width: 992px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .sidebar-section {
    order: -1; /* 사이드바를 인기 게시글 위로 올림 */
    margin-bottom: 2rem; /* 사이드바와 인기 게시글 사이 간격 */
  }
  .popular-posts-section {
    margin-bottom: 0; /* 기존 간격 제거 */
  }
}

@media (max-width: 768px) {
  .container {
    padding: 1.5rem 1rem; /* 모바일 컨테이너 패딩 */
  }

  /* 모바일 히어로 섹션 스타일 */
  .hero-section {
    height: 200px;
    /* border-radius: 0; 제거 */
    /* margin-top: 1.5rem; 제거 */
  }

  .hero-text-content {
    padding: 1rem 1.5rem;
  }

  .hero-text-content h2 {
    font-size: 1.3rem;
  }

  .hero-text-content p {
    font-size: 0.9rem;
  }

  /* 모바일 광고 섹션 간격 */
  .ad-section {
    margin-top: 1.5rem;
    margin-bottom: 1.5rem;
  }

  /* 모바일 인기 게시글 헤더 */
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
    flex-grow: 1;
    text-align: center;
  }

  .posts-grid {
    grid-template-columns: 1fr;
  }

  .sidebar-section {
    margin-bottom: 1.5rem; /* 모바일 사이드바와 인기글 간격 조정 */
  }

}
</style>
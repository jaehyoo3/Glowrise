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
              <div v-if="isPostsLoading" class="loading-state">
                <span>Loading popular posts...</span>
              </div>
              <div v-else-if="!popularPosts || popularPosts.length === 0" class="placeholder-posts"><p>Trending content
                coming soon...</p>
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
            <div v-if="isLoading" class="loading-state">
              <span>Loading...</span>
            </div>
            <div v-else-if="!isLoggedIn" class="login-prompt">
              <h3>Start Your Blog</h3>
              <p>Sign in to create your personal space</p>
              <p style="margin-top: 1rem; font-size: 0.9em; color: #555;">Please use the login button above.</p>
            </div>
            <div v-else-if="!blog" class="create-blog-prompt">
              <h3>Your Blog</h3>
              <p>You haven't created a blog yet.</p>
              <router-link v-if="userNickname" class="btn-secondary" to="/blog/create">Create Blog</router-link>
              <p v-else>Please set your nickname first. (Check user menu in NavBar)</p>
            </div>
            <div v-else class="blog-info">
              <h3>{{ blog.title }}</h3>
              <div class="blog-actions">
                <router-link :to="`/${blog.url}`" class="btn-primary">View Blog</router-link>
                <router-link v-if="blog.id" :to="`/blog/edit/${blog.id}`" class="btn-secondary">Edit</router-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue'; // NavBar 컴포넌트 임포트
import authService from '@/services/authService'; // 인증 서비스 임포트

export default {
  name: 'HomeView',
  components: {NavBar}, // 컴포넌트 등록
  data() {
    return {
      blog: null,              // 로그인한 사용자의 블로그 정보
      isLoading: true,         // 사이드바 블로그 정보 로딩 상태
      isLoggedIn: false,       // 로그인 여부 상태
      userNickname: '',        // 로그인한 사용자의 닉네임 (사이드바 조건부 렌더링용)
      popularPosts: [],        // 인기 게시글 목록 (항상 배열로 유지)
      isPostsLoading: true,    // 인기 게시글 로딩 상태
      selectedPeriod: 'WEEKLY',// 인기 게시글 기간 (기본값: 주간)
      // --- 닉네임 설정 모달 관련 data 제거됨 ---
    };
  },
  async created() {
    // 컴포넌트 생성 시 데이터 로드
    await this.loadBlogAndCheckLogin();
    // 로그인 상태가 확정된 후 인기 게시글 로드 (선택적)
    // 또는 병렬 로드 후 로그인 상태에 따라 UI 제어
    await this.loadPopularPosts();
    // --- 닉네임 설정 모달 관련 checkShowNicknamePrompt() 호출 제거됨 ---
  },
  methods: {
    // 로그인 상태 확인 및 블로그 정보 로드를 통합
    async loadBlogAndCheckLogin() {
      this.isLoading = true; // 사이드바 로딩 시작
      try {
        const storedUser = authService.getStoredUser();
        this.isLoggedIn = !!storedUser; // 로그인 상태 설정
        this.userNickname = storedUser?.nickName || ''; // 닉네임 상태 설정

        if (this.isLoggedIn) {
          // 최신 사용자 정보 가져오기 (닉네임 포함)
          try {
            const currentUser = await authService.getCurrentUser();
            if (currentUser) {
              // authService.getCurrentUser에서 로컬 스토리지를 업데이트 했으므로 다시 읽기
              const updatedUser = authService.getStoredUser();
              this.userNickname = updatedUser?.nickName || ''; // 최신 닉네임
              // 필요 시 다른 정보도 업데이트: this.isLoggedIn = true; 등
            } else {
              // getCurrentUser가 null 반환 시 (로그아웃 처리됨)
              this.isLoggedIn = false;
              this.userNickname = '';
              this.blog = null;
            }
          } catch (authError) {
            console.warn("홈 화면: 최신 사용자 정보 로드 실패", authError);
            if (authError.response?.status === 401 || authError.response?.status === 403) {
              this.isLoggedIn = false;
              this.userNickname = '';
              this.blog = null;
            }
            // 그 외 에러는 로컬 정보 기반으로 진행
          }

          // 최종 로그인 상태 확인 후 블로그 로드
          if (this.isLoggedIn) {
            try {
              const blogData = await authService.getBlogByUserId();
              this.blog = blogData;
            } catch (blogError) {
              // 블로그 정보 로드 실패 (404는 정상일 수 있음)
              if (blogError.response?.status !== 404) {
                console.error("홈 화면: 블로그 정보 로드 에러", blogError);
              }
              this.blog = null;
            }
          } else {
            this.blog = null; // 로그인 상태 아니면 블로그 없음
          }

        } else {
          this.blog = null; // 비회원
        }
      } catch (error) { // loadBlogAndCheckLogin 전체의 예외 처리 (거의 발생 안 함)
        console.error('홈 화면: 초기 데이터 로드 중 예외 발생', error);
        this.isLoggedIn = false;
        this.userNickname = '';
        this.blog = null;
      } finally {
        this.isLoading = false; // 사이드바 로딩 완료
      }
    },

    // --- 닉네임 설정 모달 관련 메소드 제거됨 ---

    // 인기 게시글 로드 (Array.isArray 추가된 버전)
    async loadPopularPosts() {
      try {
        this.isPostsLoading = true;
        const popularData = await authService.getPopularPosts(this.selectedPeriod);
        console.log("Received from getPopularPosts:", popularData); // API 응답 데이터 확인용 로그

        // *** 데이터 타입 확인 및 안전한 할당 ***
        if (Array.isArray(popularData)) {
          this.popularPosts = popularData; // 배열이면 그대로 할당
        } else {
          console.warn("getPopularPosts did not return an array:", popularData);
          this.popularPosts = []; // 배열이 아니면 빈 배열 할당
        }
        // *** 수정 끝 ***

      } catch (error) {
        console.error('인기 게시글 로드 실패:', error);
        this.popularPosts = []; // 에러 발생 시에도 빈 배열 할당
      } finally {
        this.isPostsLoading = false;
      }
    },

    // 게시글 내용 요약 (HTML 태그 제거 포함)
    truncateContent(content) {
      if (!content) return '';
      // 간단한 HTML 태그 제거
      const textContent = content.replace(/<[^>]*>/g, '');
      const maxLength = 80; // 최대 글자 수
      return textContent.length > maxLength ? textContent.substring(0, maxLength) + '...' : textContent;
    },

    // 날짜 포맷팅
    formatDate(dateString) {
      if (!dateString) return '';
      try {
        const date = new Date(dateString);
        // 'YYYY. MM. DD.' 형식 (toLocaleDateString 옵션 활용)
        return date.toLocaleDateString('ko-KR', {year: 'numeric', month: '2-digit', day: '2-digit'}).replace(/\.$/, '');
      } catch (e) {
        console.error("날짜 포맷팅 오류:", e);
        return dateString; // 오류 시 원본 반환
      }
    },

    // 인기 게시글 클릭 시 상세 페이지 이동
    navigateToPost(post) { // post 객체를 받도록 수정
      // 백엔드 응답에 blogUrl, menuId가 포함되어 있다고 가정
      if (post && post.blogUrl && post.menuId && post.id) {
        this.$router.push(`/${post.blogUrl}/${post.menuId}/${post.id}`);
      } else {
        console.warn("navigateToPost: 게시글 상세 정보 부족.", post);
        // postId만으로 조회 가능한 대체 경로가 있다면 사용
        // 예: this.$router.push(`/posts/${post.id}`);
      }
    },

    // 인기 게시글 기간 변경
    async changeTimePeriod(period) {
      if (this.selectedPeriod === period) return; // 이미 선택된 기간이면 무시
      this.selectedPeriod = period;
      await this.loadPopularPosts(); // 변경된 기간으로 다시 로드
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
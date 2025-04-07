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
              <div v-else-if="popularPosts.length === 0" class="placeholder-posts">
                <p>Trending content coming soon...</p>
              </div>
              <div v-else class="posts-grid">
                <div
                    v-for="post in popularPosts"
                    :key="post.id"
                    class="post-card"
                    @click="navigateToPost(post.id)">
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
              <router-link to="/login" class="btn-primary">Sign In</router-link>
            </div>
            <div v-else-if="!blog" class="create-blog-prompt">
              <h3>Your Blog</h3>
              <p>You haven't created a blog yet</p>
              <router-link v-if="userNickname" class="btn-secondary" to="/blog/create">Create Blog</router-link>
              <p v-else>Please set your nickname first.</p>
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

      <div v-if="showNicknameModal" class="modal-overlay" @click.self="closeNicknameModal">
        <div class="modal-content">
          <h2>닉네임 설정</h2>
          <p>블로그 활동에 사용할 닉네임을 설정해주세요. (2~15자)</p>
          <form @submit.prevent="submitNickname">
            <div class="form-group">
              <input
                  id="newNickname"
                  ref="nicknameInput"
                  v-model.trim="newNickname"
                  aria-label="새 닉네임 입력"
                  maxlength="15"
                  minlength="2"
                  pattern="^[a-zA-Z0-9가-힣_]+$"
                  placeholder="영문, 숫자, 한글, _ 만 사용 가능"
                  required type="text"
              />
              <div v-if="nicknameInputError" class="error-message modal-error">{{ nicknameInputError }}</div>
            </div>
            <div v-if="nicknameServerError" class="error-message modal-error">{{ nicknameServerError }}</div>
            <div class="modal-actions">
              <button class="btn-secondary modal-btn" type="button" @click="closeNicknameModal">닫기</button>
              <button :disabled="isSettingNickname" class="btn-primary modal-btn" type="submit">
                {{ isSettingNickname ? '저장 중...' : '닉네임 저장' }}
              </button>
            </div>
          </form>
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
      userNickname: '',        // 로그인한 사용자의 닉네임 (사이드바 조건부 렌더링 등)
      popularPosts: [],        // 인기 게시글 목록
      isPostsLoading: true,    // 인기 게시글 로딩 상태
      selectedPeriod: 'WEEKLY',// 인기 게시글 기간 (기본값: 주간)

      // --- 닉네임 설정 모달 관련 데이터 ---
      showNicknameModal: false,    // 모달 표시 여부
      newNickname: '',           // 입력된 새 닉네임
      nicknameInputError: '',    // 입력 유효성 에러 메시지
      nicknameServerError: '',   // 서버 응답 에러 메시지
      isSettingNickname: false   // 저장 버튼 로딩 상태
      // --- 모달 데이터 끝 ---
    };
  },
  async created() {
    // 컴포넌트 생성 시 데이터 로드 순서 정의
    await this.loadBlogAndCheckLogin(); // 로그인 상태 확인 및 블로그 정보 로드
    await this.loadPopularPosts();    // 인기 게시글 로드
    // 로그인 상태가 확정된 후 닉네임 프롬프트 확인
    this.$nextTick(() => {
      this.checkShowNicknamePrompt();
    });
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
          // (선택적) 최신 사용자 정보 가져오기 (닉네임 포함)
          try {
            const currentUser = await authService.getCurrentUser();
            if (currentUser) {
              this.userNickname = currentUser.nickName || ''; // 최신 닉네임으로 업데이트
            } else {
              // getCurrentUser가 null 반환 시 로그아웃 처리된 것일 수 있음
              this.isLoggedIn = false;
              this.userNickname = '';
            }
          } catch (authError) {
            console.warn("홈 화면: 최신 사용자 정보 로드 실패", authError);
            if (authError.response?.status === 401 || authError.response?.status === 403) {
              this.isLoggedIn = false; // 인증 오류 시 로그아웃 상태로 간주
              this.userNickname = '';
            }
            // 로컬 스토리지 데이터 기반으로 계속 진행하거나, 에러 처리
          }

          // 로그인 상태가 여전히 유효하면 블로그 정보 로드
          if (this.isLoggedIn) {
            const blogData = await authService.getBlogByUserId();
            this.blog = blogData;
          } else {
            this.blog = null; // getCurrentUser 실패 등으로 로그아웃 처리된 경우
          }

        } else {
          this.blog = null; // 비회원
        }
      } catch (error) {
        console.error('홈 화면: 블로그 정보 로드 중 에러', error);
        this.blog = null;
        // 에러 종류에 따라 로그인 상태 재확인 필요 시 로직 추가
        if (error.response?.status === 401 || error.response?.status === 403) {
          this.isLoggedIn = false;
          this.userNickname = '';
        }
      } finally {
        this.isLoading = false; // 사이드바 로딩 완료
      }
    },

    // LocalStorage 플래그 확인 후 모달 표시
    checkShowNicknamePrompt() {
      // 로그인 상태이고, 플래그가 true일 때 모달 표시
      if (this.isLoggedIn && localStorage.getItem('showNicknamePrompt') === 'true') {
        console.log("HomeView: 닉네임 설정 프롬프트 표시 조건 충족");
        this.showNicknameModal = true;
        localStorage.removeItem('showNicknamePrompt'); // 플래그는 즉시 제거

        // 모달이 화면에 렌더링된 후 입력 필드에 포커스
        this.$nextTick(() => {
          if (this.$refs.nicknameInput) {
            this.$refs.nicknameInput.focus();
          }
        });
      }
    },

    // 닉네임 저장 로직
    async submitNickname() {
      this.nicknameInputError = '';
      this.nicknameServerError = '';

      // 클라이언트 측 유효성 검사
      if (!this.validateNicknameInput()) {
        return;
      }

      this.isSettingNickname = true;
      try {
        // 백엔드 API 호출하여 닉네임 업데이트
        await authService.updateUserNickname(this.newNickname);
        alert('닉네임이 성공적으로 설정되었습니다!');
        this.closeNicknameModal(); // 성공 시 모달 닫기

        // 중요: 닉네임 변경 후 관련 상태 갱신
        // 1. 로컬 스토리지 및 현재 컴포넌트 상태 업데이트
        this.userNickname = this.newNickname; // 현재 컴포넌트 닉네임 상태 업데이트
        // authService.updateUserNickname 내부에서 로컬 스토리지가 업데이트 되었다고 가정
        // 필요 시 여기서 authService.getCurrentUser() 재호출하여 로컬 스토리지 명시적 갱신

        // 2. NavBar 등 다른 컴포넌트가 갱신된 정보를 반영하도록 처리
        //    - 페이지 새로고침 (가장 간단하지만 사용자 경험 저하)
        //    - 전역 이벤트 발생시켜 NavBar가 다시 로드하도록 유도
        //    - Vuex/Pinia 사용 시 상태 변경
        window.location.reload(); // <<<--- 가장 간단한 방법: 페이지 새로고침

        // 새로고침 대신 다른 방법을 사용하려면 아래 주석 참고
        /*
        // 예시: Event Bus 사용 (eventBus가 설정되어 있다고 가정)
        // import eventBus from '@/eventBus';
        // eventBus.emit('user-profile-updated');
        // NavBar는 이 이벤트를 수신하여 checkLoginStatus() 호출
        */

        /*
        // 예시: Vuex/Pinia 사용 (store가 설정되어 있다고 가정)
        // this.$store.dispatch('auth/fetchCurrentUser'); // 사용자 정보 다시 가져오는 액션 실행
        */

      } catch (error) {
        console.error('닉네임 저장 실패:', error);
        // 에러 메시지 표시
        if (error.response && error.response.data && error.response.data.message) {
          this.nicknameServerError = error.response.data.message; // 백엔드 에러 메시지
        } else {
          this.nicknameServerError = '닉네임 저장 중 오류가 발생했습니다.';
        }
      } finally {
        this.isSettingNickname = false; // 로딩 상태 해제
      }
    },

    // 모달 내 입력값 유효성 검사
    validateNicknameInput() {
      const nickname = this.newNickname;
      if (!nickname) {
        this.nicknameInputError = '닉네임을 입력해주세요.';
        return false;
      }
      if (nickname.length < 2 || nickname.length > 15) {
        this.nicknameInputError = '닉네임은 2자 이상 15자 이하로 입력해주세요.';
        return false;
      }
      // 정규식 검사 (영문, 숫자, 한글, 밑줄)
      const pattern = /^[a-zA-Z0-9가-힣_]+$/;
      if (!pattern.test(nickname)) {
        this.nicknameInputError = '닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.';
        return false;
      }
      this.nicknameInputError = ''; // 에러 없음
      return true;
    },

    // 모달 닫기
    closeNicknameModal() {
      this.showNicknameModal = false;
      this.newNickname = ''; // 입력 필드 초기화
      this.nicknameInputError = '';
      this.nicknameServerError = '';
    },

    // 인기 게시글 로드
    async loadPopularPosts() {
      try {
        this.isPostsLoading = true;
        this.popularPosts = await authService.getPopularPosts(this.selectedPeriod);
      } catch (error) {
        console.error('인기 게시글 로드 실패:', error);
        this.popularPosts = []; // 실패 시 빈 배열로 초기화
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

    // 인기 게시글 클릭 시 상세 페이지 이동 (경로 수정 필요)
    navigateToPost(postId) {
      // postId만으로는 완전한 경로 구성 어려움 (blogUrl, menuId 필요)
      // 백엔드 getPopularPosts 응답에 관련 정보를 포함시키거나,
      // postId만으로 조회 가능한 별도 라우트 필요
      console.warn("navigateToPost: 게시글 상세 경로 구현 필요.", postId);
      // 임시: this.$router.push(`/post/detail/${postId}`); // 상세 조회 가능한 라우트가 있다면
    },

    // 인기 게시글 기간 변경
    async changeTimePeriod(period) {
      this.selectedPeriod = period;
      await this.loadPopularPosts(); // 변경된 기간으로 다시 로드
    }
  }
};
</script>

<style scoped>
/* --- 기본 레이아웃 및 컨테이너 --- */
.home {
  background-color: #f8f9fa; /* 전체 배경색 */
  min-height: 100vh; /* 최소 높이를 화면 높이만큼 */
  color: #333; /* 기본 텍스트 색상 */
}

.container {
  max-width: 1200px; /* 최대 너비 */
  margin: 0 auto; /* 가운데 정렬 */
  padding: 2rem; /* 내부 여백 */
  box-sizing: border-box;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 2.5rem; /* 섹션 간 간격 */
}

/* --- 히어로 섹션 --- */
.hero-section {
  text-align: center;
  padding: 3rem 0; /* 위아래 패딩 */
}

.site-title {
  font-size: clamp(2.5rem, 6vw, 3.8rem); /* 반응형 폰트 크기 */
  font-weight: 700;
  color: #1a1a1a; /* 조금 더 진한 검정 */
  margin-bottom: 0.5rem;
}

.site-description {
  font-size: clamp(1rem, 3vw, 1.25rem); /* 반응형 폰트 크기 */
  color: #555; /* 약간 연한 회색 */
}

/* --- 메인 그리드 --- */
.main-grid {
  display: grid;
  /* 기본 2열, 작은 화면에서는 1열 */
  grid-template-columns: repeat(auto-fit, minmax(min(300px, 100%), 1fr));
  gap: 2rem; /* 그리드 아이템 간 간격 */
}

.blog-overview, .blog-sidebar {
  background-color: white;
  border-radius: 8px;
  padding: 2rem; /* 내부 패딩 증가 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); /* 부드러운 그림자 */
}

.blog-sidebar h3, .popular-posts h3 {
  border-bottom: 2px solid #eee; /* 구분선 연하게 */
  padding-bottom: 0.7rem;
  margin-bottom: 1.5rem; /* 제목 아래 간격 증가 */
  font-size: 1.3rem; /* 제목 크기 약간 증가 */
  color: #222;
}

/* --- 버튼 스타일 --- */
.btn-primary, .btn-secondary {
  display: inline-block;
  padding: 0.7rem 1.4rem; /* 버튼 패딩 조정 */
  border-radius: 5px; /* 약간 둥근 모서리 */
  text-decoration: none;
  font-weight: 500; /* 글씨 두께 */
  transition: all 0.2s ease;
  text-align: center;
  cursor: pointer;
  font-size: 0.95rem;
}

.btn-primary {
  background-color: #007bff; /* 기본 파란색 버튼 */
  color: white;
  border: 1px solid #007bff;
}

.btn-primary:hover {
  background-color: #0056b3;
  border-color: #0056b3;
}

.btn-secondary {
  background-color: white;
  color: #333;
  border: 1px solid #ccc;
  margin-left: 0.5rem; /* 버튼 간 간격 */
}

.btn-secondary:hover {
  background-color: #f8f9fa;
  border-color: #bbb;
}

.blog-actions {
  display: flex;
  flex-wrap: wrap; /* 작은 화면에서 버튼 줄바꿈 */
  gap: 0.75rem; /* 버튼 간 간격 */
  margin-top: 1rem;
}

/* --- 플레이스홀더 및 로딩 상태 --- */
.placeholder-posts, .loading-state {
  background-color: #f8f9fa;
  padding: 1.5rem; /* 패딩 증가 */
  border-radius: 6px; /* 모서리 둥글게 */
  text-align: center;
  color: #666;
  min-height: 100px; /* 최소 높이 */
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
  margin-top: 1rem;
}

.post-card {
  border: 1px solid #e9ecef; /* 테두리 추가 */
  border-radius: 6px;
  overflow: hidden;
  background: white; /* 배경 흰색 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
  display: flex; /* 내부 요소 정렬 위해 Flex 사용 */
  flex-direction: column; /* 세로 정렬 */
  height: 100%; /* 카드 높이 통일 */
}

.post-card:hover {
  transform: translateY(-4px); /* 호버 시 약간 위로 이동 */
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08); /* 그림자 강조 */
}

.post-content {
  padding: 1.2rem; /* 내부 패딩 증가 */
  flex-grow: 1; /* 내용 영역이 남은 공간 차지 */
  display: flex;
  flex-direction: column;
}

.post-title {
  margin: 0 0 0.6rem 0;
  font-size: 1.15rem; /* 제목 크기 약간 증가 */
  font-weight: 600;
  line-height: 1.4; /* 줄 간격 */
  color: #222;
  /* 여러 줄 말줄임 (필요 시) */
  /* display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; */
}

.post-excerpt {
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 1rem; /* 메타 정보와 간격 */
  line-height: 1.5; /* 줄 간격 */
  flex-grow: 1; /* 요약 내용이 남은 공간 차지 */
  /* 여러 줄 말줄임 (필요 시) */
  /* display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; */
}

.post-meta {
  display: flex;
  flex-wrap: wrap; /* 작은 화면에서 줄 바꿈 */
  font-size: 0.8rem;
  color: #777;
  gap: 0.8rem; /* 메타 정보 간 간격 */
  margin-top: auto; /* 메타 정보를 카드 하단에 위치 */
  padding-top: 0.5rem; /* 위쪽 내용과 약간의 간격 */
  border-top: 1px solid #f0f0f0; /* 구분선 추가 */
}

.post-meta span {
  display: flex;
  align-items: center;
  gap: 0.3rem; /* 아이콘과 텍스트 간격 */
}

.post-meta span i {
  font-size: 0.9em; /* 아이콘 크기 조정 */
}

/* --- 기간 필터 버튼 --- */
.time-period-filter {
  display: flex;
  justify-content: flex-end; /* 오른쪽 정렬 */
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.period-btn {
  border: 1px solid #ddd;
  background: #f8f9fa; /* 배경색 추가 */
  padding: 0.4rem 0.9rem; /* 패딩 조정 */
  border-radius: 20px;
  font-size: 0.8rem; /* 폰트 크기 약간 줄임 */
  cursor: pointer;
  transition: all 0.2s ease;
  color: #555;
}

.period-btn:hover {
  background: #e9ecef; /* 호버 색상 변경 */
  border-color: #ccc;
}

.period-btn.active {
  background: #007bff; /* 활성 버튼 색상 */
  color: white;
  border-color: #007bff;
  font-weight: 500;
}

/* --- 모달 스타일 --- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  padding: 1rem; /* 모바일 화면 고려 패딩 */
  box-sizing: border-box;
}

.modal-content {
  background-color: white;
  padding: 2.5rem; /* 패딩 증가 */
  border-radius: 8px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2); /* 그림자 약간 강조 */
  width: 95%; /* 모바일 화면 너비 고려 */
  max-width: 480px; /* 최대 너비 */
  text-align: center;
  box-sizing: border-box;
}

.modal-content h2 {
  margin-top: 0;
  margin-bottom: 1rem;
  font-size: 1.5rem; /* 제목 크기 */
  color: #333;
}

.modal-content p {
  margin-bottom: 1.8rem; /* 설명 아래 간격 */
  color: #555;
  font-size: 1rem;
}

.modal-content .form-group {
  margin-bottom: 1rem;
  text-align: left;
}

.modal-content .form-group input {
  width: 100%;
  padding: 0.8rem; /* 입력 필드 패딩 */
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
  box-sizing: border-box;
}

.modal-content .form-group input:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.2); /* 포커스 효과 강조 */
  outline: none;
}

.modal-error {
  font-size: 0.85rem !important;
  margin-top: 0.4rem !important;
  margin-bottom: 0 !important;
  text-align: left;
  color: #dc3545; /* 에러 색상 */
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 2rem; /* 버튼 위 간격 */
}

.modal-btn {
  padding: 0.7rem 1.4rem; /* 모달 버튼 패딩 */
  font-size: 0.95rem;
}

.modal-btn.btn-secondary { /* 모달 내 보조 버튼 스타일 */
  background-color: #6c757d;
  color: white;
  border-color: #6c757d;
}

.modal-btn.btn-secondary:hover {
  background-color: #5a6268;
  border-color: #545b62;
}


/* --- 반응형 조정 --- */
@media (max-width: 992px) {
  /* 중간 화면 크기 */
  .main-grid {
    grid-template-columns: 3fr 2fr; /* 사이드바 비율 조정 */
  }
}

@media (max-width: 768px) {
  /* 태블릿 이하 */
  .main-grid {
    grid-template-columns: 1fr; /* 1열 레이아웃 */
  }

  .blog-overview {
    order: 1; /* 블로그 개요 먼저 */
  }

  .blog-sidebar {
    order: 2; /* 사이드바 나중에 */
  }

  .posts-grid {
    grid-template-columns: 1fr; /* 모바일에서 인기글 1열 */
  }

  .time-period-filter {
    justify-content: center; /* 기간 필터 가운데 정렬 */
  }

  .modal-content {
    padding: 1.5rem; /* 모바일에서 모달 패딩 줄임 */
  }

  .modal-content h2 {
    font-size: 1.3rem;
  }

  .modal-actions {
    justify-content: space-between; /* 모바일에서 버튼 양쪽 정렬 */
  }
}
</style>
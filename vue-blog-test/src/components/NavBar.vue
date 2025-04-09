<template>
  <div class="navbar">
    <div class="container">
      <div class="navbar-brand">
        <router-link to="/" class="logo">Glowrise</router-link>
      </div>

      <div class="search-bar">
        <input
            type="text"
            placeholder="검색"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
        />
        <button class="search-button" @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>
      </div>

      <div class="navbar-menu">
        <template v-if="isLoggedIn">
          <div class="notification-menu" @click.stop="toggleNotificationDropdown">
            <i class="fas fa-bell"></i>
            <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount }}</span>

            <div v-if="showNotificationDropdown" class="dropdown-menu notification-dropdown" @click.stop>
              <div v-if="notifications.length === 0" class="dropdown-item no-notifications">
                알림이 없습니다.
              </div>
              <div v-else>
                <div
                    v-for="notification in notifications"
                    :key="notification.id"
                    class="dropdown-item notification-item"
                    :class="{ 'unread': !notification.isRead }"
                    @click="handleNotificationClick(notification)"
                >
                  <span>{{ notification.message }}</span>
                  <small>{{ formatDate(notification.createdDate) }}</small>
                </div>
              </div>
            </div>
          </div>

          <div class="user-menu" @click.stop="toggleUserDropdown">
             <span v-if="isLoggedIn" class="user-name">
               {{ nickName || userName || '사용자' }}
             </span>
            <i v-if="isLoggedIn" class="fas fa-chevron-down"></i>

            <div v-if="showUserDropdown && isLoggedIn" class="dropdown-menu" @click.stop>
              <template v-if="hasBlog">
                <router-link
                    :to="`/${blogUrl}`"
                    class="dropdown-item"
                    @click="closeDropdown"
                >
                  내 블로그
                </router-link>
              </template>
              <template v-else>
                <router-link
                    v-if="nickName" to="/blog/create"
                    class="dropdown-item"
                    @click="closeDropdown"
                >
                  블로그 만들기
                </router-link>
                <button v-else class="dropdown-item" @click="openProfileCompletionModal">닉네임 설정하기</button>
              </template>

              <router-link
                  class="dropdown-item"
                  to="/profile/edit"
                  @click="closeDropdown"
              >
                내 정보 수정
              </router-link>

              <div class="dropdown-divider"></div>
              <button
                  @click="handleLogout"
                  class="dropdown-item logout-btn"
                  :disabled="isLoggingOut"
              >
                {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
              </button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="auth-buttons">
            <button class="signup-button" @click="openSignupModal">회원가입</button>
            <button class="login-button" @click="openLoginModal">로그인</button>
          </div>
        </template>
      </div>
    </div>

    <LoginSignupModal
        v-if="showLoginModal"
        :initial-tab="modalInitialTab"
        :oauth-completion-data="oauthCompletionDataForModal"
        @close="closeLoginModal"
        @login-success="handleAuthSuccess"
        @profile-completed="handleAuthSuccess"/>
  </div>
</template>

<script>
// --- Vuex 헬퍼 함수 import ---
import {mapActions, mapGetters, mapState} from 'vuex';
// ---------------------------
import authService from '@/services/authService'; // 알림 등 직접 API 호출 여전히 필요
import {websocketService} from '@/services/websocketService';
import LoginSignupModal from '@/components/LoginSignupModal.vue';

export default {
  name: 'NavBar',
  components: {LoginSignupModal},
  data() {
    return {
      searchQuery: '',
      showUserDropdown: false,
      showNotificationDropdown: false,
      // --- 로컬 상태 제거: isLoggedIn, userId, userName, nickName, userEmail, userProfileImage, hasBlog, blogUrl ---
      isLoggingOut: false, // 로그아웃 진행 상태 표시는 유지 가능
      notifications: [], // 알림 목록은 컴포넌트 로컬 상태로 관리
      unreadCount: 0, // 안 읽은 알림 수도 로컬 상태
      showLoginModal: false, // 모달 표시 여부
      modalInitialTab: 'login', // 모달 초기 탭
      oauthCompletionDataForModal: null, // OAuth 프로필 완료용 데이터
    };
  },
  computed: {
    // --- Vuex Getters/State 매핑 ---
    ...mapState([
      // 필요하다면 state 직접 접근: 'currentUser', 'userBlog'
    ]),
    ...mapGetters([
      'isLoggedIn',       // 로그인 여부
      'userId',           // 사용자 ID (WebSocket 연결 등에 사용)
      'username',         // 사용자 이름 (표시용)
      'nickName',         // 닉네임 (표시용)
      'userEmail',        // 이메일 (프로필 완료 모달 전달용)
      'userProfileImage', // 프로필 이미지 URL (표시용)
      'blogUrl',          // 사용자 블로그 URL (링크용)
      'hasBlog',          // 블로그 존재 여부 (링크 표시 제어용)
      'isLoading'         // 스토어 로딩 상태 (전체 로딩 인디케이터 등에 활용 가능)
    ]),
    // ---------------------------

    // 프로필 이미지 URL 기본값 처리
    displayProfileImage() {
      return this.userProfileImage || '/default-profile.png'; // 기본 이미지 경로 설정
    },
    displayNicknameOrUsername() {
      return this.nickName || this.username; // 닉네임 없으면 username 표시
    }
  },
  watch: {
    // --- Vuex 상태 변경 감지 ---
    isLoggedIn(newValue, oldValue) {
      console.log("NavBar Watcher: isLoggedIn 변경됨", oldValue, "->", newValue);
      if (newValue) {
        // 로그인 상태가 되면 필요한 작업 수행
        this.fetchNotifications(); // 알림 가져오기
        this.connectWebSocket();   // 웹소켓 연결
      } else {
        // 로그아웃 상태가 되면 정리 작업 수행
        this.notifications = [];   // 알림 목록 초기화
        this.unreadCount = 0;
        websocketService.disconnect(); // 웹소켓 연결 해제
        console.log("NavBar Watcher: 로그아웃됨, 알림/WS 정리");
      }
    }
    // ------------------------
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
    // --- 삭제: checkOAuthCompletionOnLoad 는 created 또는 watch isLoggedIn 에서 처리 가능 ---
    // this.checkOAuthCompletionOnLoad(); // 아래 created로 이동 또는 삭제
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    // 컴포넌트 제거 시 웹소켓 연결 해제
    if (websocketService) websocketService.disconnect();
  },
  created() {
    // --- 삭제: checkLoginStatus 호출 제거 ---
    // Vuex 스토어는 main.js에서 이미 초기화 시도함
    // 컴포넌트 생성 시 필요한 초기 작업 (예: OAuth 완료 데이터 확인)
    this.checkOAuthCompletionOnLoad(); // 여기서 호출하거나 watch isLoggedIn 사용
  },
  methods: {
    // --- Vuex Actions 매핑 ---
    ...mapActions(['fetchCurrentUser', 'logoutAndClear']),
    // ------------------------

    handleOutsideClick(event) {
      // 드롭다운 외부 클릭 감지 로직 (기존과 동일)
      const userMenu = this.$el.querySelector('.user-menu');
      const notificationMenu = this.$el.querySelector('.notification-menu');
      let clickedInsideUserMenu = userMenu?.contains(event.target);
      let clickedInsideNotificationMenu = notificationMenu?.contains(event.target);

      if (!clickedInsideUserMenu) this.showUserDropdown = false;
      if (!clickedInsideNotificationMenu) this.showNotificationDropdown = false;
    },
    toggleUserDropdown() {
      this.showUserDropdown = !this.showUserDropdown;
      if (this.showUserDropdown) this.showNotificationDropdown = false;
    },
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      if (this.showNotificationDropdown) {
        this.showUserDropdown = false;
        // --- 로그인 상태일 때만 알림 가져오기 ---
        if (this.isLoggedIn) {
          this.fetchNotifications();
        }
        // -----------------------------------
      }
    },
    closeDropdown() {
      this.showUserDropdown = false;
      this.showNotificationDropdown = false;
    },

    // --- 삭제: checkLoginStatus, resetAuthStates, checkBlogStatus 메서드 ---

    async fetchNotifications() {
      // 로그인 상태 확인 (getter 사용)
      if (!this.isLoggedIn) return;
      try {
        console.log("NavBar: 알림 가져오는 중...");
        // authService 직접 호출 유지 (알림은 스토어에서 관리 안 함)
        const fetchedNotifications = await authService.getNotifications();
        this.notifications = fetchedNotifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
        console.log("NavBar: 알림 가져오기 완료. Count:", this.notifications.length, "Unread:", this.unreadCount);
      } catch (error) {
        console.error('NavBar: 알림 가져오기 실패:', error);
        this.notifications = [];
        this.unreadCount = 0;
      }
    },
    connectWebSocket() {
      // 로그인 상태 및 userId 확인 (getter 사용)
      if (!this.isLoggedIn || !this.userId) {
        console.log("NavBar: WebSocket 연결 조건 미충족. isLoggedIn:", this.isLoggedIn, "userId:", this.userId);
        return;
      }
      console.log("NavBar: WebSocket 연결 시도. userId:", this.userId);
      websocketService.disconnect(); // 이전 연결 정리
      websocketService.connect(this.userId, (notification) => {
        // 웹소켓 알림 수신 처리 로직 (기존과 동일)
        console.log('NavBar: WebSocket 알림 수신:', notification);
        if (!this.notifications.some(n => n.id === notification.id)) {
          this.notifications.unshift(notification);
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
          if (!notification.isRead) {
            this.unreadCount += 1;
          }
          console.log('NavBar: 새 알림 추가됨. Unread count:', this.unreadCount);
        }
      });
    },
    async handleNotificationClick(notification) {
      // 알림 클릭 처리 로직 (기존과 동일, authService 직접 사용)
      if (!notification || !notification.id) return;
      console.log("NavBar: 알림 클릭됨:", notification.id, "isRead:", notification.isRead);
      try {
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          await this.fetchNotifications(); // 목록 갱신
        }
        if (notification.blogUrl && notification.menuId && notification.postId) {
          const targetPath = `/${notification.blogUrl}/${notification.menuId}/${notification.postId}`;
          this.$router.push(targetPath).catch({});
        }
        this.closeDropdown();
      } catch (error) {
        console.error('NavBar: 알림 처리 실패:', error);
        alert('알림을 처리하는 중 오류가 발생했습니다.');
      }
    },
    formatDate(dateString) {
      // 날짜 포맷팅 (기존과 동일)
      if (!dateString) return '';
      try {
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR', { /* ... */});
      } catch (e) {
        return dateString;
      }
    },
    async handleLogout() {
      console.log("NavBar: 로그아웃 시작");
      this.isLoggingOut = true;
      try {
        // --- Vuex 액션 디스패치로 변경 ---
        await this.logoutAndClear(); // 스토어 액션 호출 (백엔드 호출 + 스토어 상태 초기화 + 로컬 스토리지 정리 포함)
        // -----------------------------
        console.log("NavBar: 스토어 로그아웃 액션 완료");
        // 글로벌 이벤트 발행 (다른 컴포넌트 알림용, 선택적)
        window.dispatchEvent(new CustomEvent('auth-state-changed'));
        console.log("NavBar: auth-state-changed 이벤트 발행 (로그아웃)");
        // 페이지 이동 (상태 변경 감지로 UI가 업데이트되므로 nextTick 불필요할 수 있음)
        if (this.$route.path !== '/') {
          this.$router.push('/');
        }
      } catch (error) {
        console.error('NavBar: 로그아웃 실패:', error);
        // 스토어 액션 내에서 에러 처리가 되지만, UI 피드백은 여기서 줄 수 있음
        alert('로그아웃 처리 중 오류가 발생했습니다.');
      } finally {
        this.isLoggingOut = false;
        this.closeDropdown();
      }
    },
    handleSearch() {
      // 검색 로직 (기존과 동일)
      const query = this.searchQuery.trim();
      if (query) {
        this.$router.push({path: '/search', query: {q: query}}).catch({});
        this.searchQuery = '';
      }
    },
    checkOAuthCompletionOnLoad() {
      // OAuth 완료 데이터 확인 로직 (기존과 동일)
      const completionDataString = sessionStorage.getItem('oauth_profile_completion');
      if (completionDataString) {
        try {
          this.oauthCompletionDataForModal = JSON.parse(completionDataString);
          console.log("NavBar: OAuth 프로필 완료 감지, 모달 자동 실행", this.oauthCompletionDataForModal);
          this.modalInitialTab = 'signup';
          this.showLoginModal = true;
          sessionStorage.removeItem('oauth_profile_completion');
        } catch (e) {
          console.error("NavBar: sessionStorage 데이터 파싱 오류", e);
          sessionStorage.removeItem('oauth_profile_completion');
          this.oauthCompletionDataForModal = null;
        }
      } else {
        this.oauthCompletionDataForModal = null;
      }
    },
    openLoginModal() {
      // 로그인 모달 열기 (기존과 동일)
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'login';
      this.showLoginModal = true;
    },
    openSignupModal() {
      // 회원가입 모달 열기 (기존과 동일)
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
    },
    openProfileCompletionModal() {
      // 프로필 완료 모달 열기 (스토어 getter 사용)
      console.log("NavBar: 프로필 완료 모달 열기 요청 (드롭다운 메뉴)");
      this.oauthCompletionDataForModal = {
        // --- 스토어 getter 사용 ---
        email: this.userEmail,
        oauthName: this.username // username 사용 또는 다른 필드 확인
        // ------------------------
      };
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.closeDropdown();
    },
    closeLoginModal() {
      // 모달 닫기 (기존과 동일)
      this.showLoginModal = false;
      this.oauthCompletionDataForModal = null;
    },
    async handleAuthSuccess() {
      // 모달에서 로그인 또는 프로필 완료 성공 시 호출됨
      console.log("NavBar: 모달 인증 성공/완료 수신. 스토어 상태 갱신 시도.");
      this.closeLoginModal(); // 모달 닫기

      try {
        // --- 스토어 액션 디스패치 ---
        // 로그인 또는 프로필(닉네임) 완료 후, 최신 사용자/블로그 정보를 스토어에 로드
        await this.fetchCurrentUser(); // 스토어 액션 호출
        // --------------------------
        console.log("NavBar: 스토어 fetchCurrentUser 완료 후. auth-state-changed 이벤트 발행.");
        // 글로벌 이벤트 발행 (선택적)
        window.dispatchEvent(new CustomEvent('auth-state-changed'));
      } catch (error) {
        console.error("NavBar: handleAuthSuccess 내 fetchCurrentUser 중 에러 발생:", error);
        // 사용자에게 에러 알림 등
      }
    }
  }
};
</script>
<style scoped>
.navbar {
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  box-sizing: border-box;
}

.navbar-brand .logo {
  font-size: 1.75rem;
  font-weight: 700;
  color: #000;
  text-decoration: none;
  letter-spacing: -0.5px;
}

.search-bar {
  flex-grow: 1;
  max-width: 450px;
  position: relative;
  margin: 0 2rem;
}

.search-bar input {
  width: 100%;
  padding: 0.6rem 2.5rem 0.6rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 0.9rem;
  background-color: #f8f9fa;
  box-sizing: border-box;
  transition: border-color 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.search-bar input:focus {
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
  outline: none;
}

.search-button {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  color: #666;
  cursor: pointer;
  padding: 5px;
}

.navbar-menu {
  display: flex;
  align-items: center;
}

.notification-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  margin-right: 1.5rem;
  padding: 5px;
  z-index: 1001;
}

.notification-menu i.fa-bell {
  font-size: 1.3rem;
  color: #333;
}

.notification-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  background-color: #333;
  color: white;
  border-radius: 50%;
  padding: 2px 5px;
  font-size: 0.7rem;
  font-weight: 500;
  line-height: 1;
  min-width: 16px;
  text-align: center;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 160px;
  padding: 0.5rem 0;
  z-index: 1000;
  display: block;
  list-style: none;
  margin: 0;
}

.notification-dropdown {
  width: 300px;
  max-height: 400px;
  overflow-y: auto;
}

.dropdown-item {
  display: block;
  padding: 0.7rem 1.2rem;
  color: #333;
  text-decoration: none;
  font-size: 0.9rem;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  transition: background-color 0.15s ease-in-out;
  white-space: normal;
  box-sizing: border-box;
  cursor: pointer;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.notification-item {
  display: flex;
  flex-direction: column;
  padding: 0.8rem 1.2rem;
  border-bottom: 1px solid #f0f0f0;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background-color: #f5f5f5;
  font-weight: 500;
}

.notification-item.unread:hover {
  background-color: #ebebeb;
}

.notification-item span {
  margin-bottom: 0.3rem;
  line-height: 1.4;
  word-break: break-word;
}

.notification-item small {
  color: #777;
  font-size: 0.75rem;
}

.no-notifications {
  padding: 1rem 1.2rem;
  color: #888;
  text-align: center;
  font-size: 0.9rem;
}

.user-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  margin-left: 0.5rem;
  padding: 5px;
  z-index: 1001;
}

.user-name {
  font-size: 0.95rem;
  font-weight: 500;
  color: #333;
  margin-right: 0.5rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
  display: inline-block;
  vertical-align: middle;
  line-height: 1;
}

.user-menu i.fa-chevron-down {
  font-size: 0.8rem;
  color: #666;
  transition: transform 0.2s ease-in-out;
  vertical-align: middle;
}

.dropdown-divider {
  height: 1px;
  background-color: #eee;
  margin: 0.5rem 0;
}

button.dropdown-item {
  color: #333;
}

button.dropdown-item.logout-btn {
  color: #555;
  font-weight: 500;
}

button.dropdown-item:hover:not(:disabled) {
  background-color: #f5f5f5;
}

button.dropdown-item:disabled {
  color: #aaa;
  cursor: not-allowed;
  background-color: transparent;
}

.auth-buttons {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.login-button, .signup-button {
  font-weight: 500;
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-button {
  color: #333;
  background-color: white;
  border: 1px solid #333;
}

.login-button:hover {
  background-color: #f5f5f5;
}

.signup-button {
  color: white;
  background-color: #333;
  border: 1px solid #333;
}

.signup-button:hover {
  background-color: #000;
}

@media (max-width: 768px) {
  .container {
    flex-wrap: wrap;
    justify-content: center;
    padding: 0 1rem;
  }

  .navbar-brand {
    width: 100%;
    text-align: center;
    margin-bottom: 1rem;
  }

  .search-bar {
    order: 3;
    max-width: 100%;
    margin: 1rem 0 0;
  }

  .navbar-menu {
    order: 2;
    margin-top: 0.5rem;
    width: 100%;
    justify-content: center;
  }

  .user-menu, .notification-menu {
    margin: 0 0.5rem;
  }

  .auth-buttons {
    width: 100%;
    justify-content: center;
    margin-top: 0.5rem;
  }

  .login-button, .signup-button {
    flex: 1;
    max-width: 120px;
    text-align: center;
  }
}
</style>
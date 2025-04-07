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
          <button class="login-button" @click="openLoginModal">로그인</button>
        </template>
      </div>
    </div>

    <LoginSignupModal
        v-if="showLoginModal"
        :initial-tab="modalInitialTab"
        :oauth-completion-data="oauthCompletionDataForModal"
        @close="closeLoginModal"
        @login-success="handleAuthSuccess"
        @profile-completed="handleAuthSuccess"
    />
  </div>
</template>

<script>
import authService from '@/services/authService';
import {websocketService} from '@/services/websocketService';
import LoginSignupModal from '@/components/LoginSignupModal.vue'; // 모달 컴포넌트 임포트 경로 확인

export default {
  name: 'NavBar',
  components: {LoginSignupModal}, // 모달 컴포넌트 등록
  data() {
    return {
      searchQuery: '',
      showUserDropdown: false,
      showNotificationDropdown: false,
      isLoggedIn: false,
      userId: null,
      userName: '',
      nickName: '',
      userEmail: '', // OAuth 완료 데이터 전달 및 상태 확인용
      userProfileImage: '',
      isLoggingOut: false,
      hasBlog: false,
      blogUrl: '',
      notifications: [],
      unreadCount: 0,

      // --- 모달 관련 상태 ---
      showLoginModal: false,               // 모달 표시 여부
      modalInitialTab: 'login',          // 모달 열 때 기본 탭
      oauthCompletionDataForModal: null, // 모달에 전달할 OAuth 데이터
    };
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
    // 페이지 로드 시 OAuth 프로필 완료 필요한지 확인
    this.checkOAuthCompletionOnLoad();
    // *** 전역 상태 관리 또는 이벤트 버스 구독 설정 위치 ***
    // 예: eventBus.$on('auth-changed', this.checkLoginStatus);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    if (websocketService) websocketService.disconnect();
    // *** 전역 상태 관리 또는 이벤트 버스 구독 해제 위치 ***
    // 예: eventBus.$off('auth-changed', this.checkLoginStatus);
  },
  created() {
    // 컴포넌트 생성 시 로그인 상태 확인 (초기 로드)
    this.checkLoginStatus();
  },
  methods: {
    // 드롭다운 외부 클릭 시 닫기
    handleOutsideClick(event) {
      const userMenu = this.$el.querySelector('.user-menu');
      const notificationMenu = this.$el.querySelector('.notification-menu');
      if (userMenu && !userMenu.contains(event.target) &&
          notificationMenu && !notificationMenu.contains(event.target)) {
        this.showUserDropdown = false;
        this.showNotificationDropdown = false;
      }
    },
    // 사용자 메뉴 토글
    toggleUserDropdown() {
      this.showUserDropdown = !this.showUserDropdown;
      if (this.showUserDropdown) this.showNotificationDropdown = false;
    },
    // 알림 메뉴 토글
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      if (this.showNotificationDropdown) {
        this.showUserDropdown = false;
        this.fetchNotifications(); // 알림 메뉴 열 때 갱신
      }
    },
    // 드롭다운 닫기
    closeDropdown() {
      this.showUserDropdown = false;
      this.showNotificationDropdown = false;
    },
    // 로그인 상태 및 사용자 정보 확인/업데이트
    async checkLoginStatus() {
      const user = authService.getStoredUser();
      if (user && user.id) {
        if (!this.isLoggedIn) this.isLoggedIn = true;
        this.userId = user.id;
        this.userName = user.username;
        this.nickName = user.nickName || '';
        this.userEmail = user.email || '';
        this.userProfileImage = user.profileImage || '';

        try {
          const freshUserData = await authService.getCurrentUser();
          if (freshUserData) {
            const updatedUser = authService.getStoredUser();
            if (updatedUser) {
              this.nickName = updatedUser.nickName || '';
              this.userEmail = updatedUser.email || '';
              this.userName = updatedUser.username;
              this.userId = updatedUser.id;
              this.isLoggedIn = true;
            } else {
              this.resetAuthStates();
              return;
            }
          } else {
            this.resetAuthStates();
            return;
          }
        } catch (error) {
          console.warn("NavBar: 최신 사용자 정보 가져오기 실패", error);
          if (error.response?.status === 401 || error.response?.status === 403) {
            this.resetAuthStates();
            return;
          }
          // 네트워크 오류 등은 일단 로그인 상태 유지 시도
        }

        if (this.isLoggedIn) {
          await this.checkBlogStatus();
          await this.fetchNotifications();
          this.connectWebSocket();
        }

      } else {
        if (this.isLoggedIn) this.resetAuthStates();
      }
    },
    // 인증 관련 상태 초기화
    resetAuthStates() {
      this.isLoggedIn = false;
      this.userId = null;
      this.userName = '';
      this.nickName = '';
      this.userEmail = '';
      this.userProfileImage = '';
      this.hasBlog = false;
      this.blogUrl = '';
      this.notifications = [];
      this.unreadCount = 0;
      if (websocketService) websocketService.disconnect();
    },
    // 블로그 소유 상태 확인
    async checkBlogStatus() {
      if (!this.userId || !this.isLoggedIn) {
        this.hasBlog = false;
        this.blogUrl = '';
        return;
      }
      try {
        const response = await authService.getBlogByUserId();
        this.hasBlog = response !== null && response.id !== undefined;
        this.blogUrl = response?.url || '';
      } catch (error) {
        if (error.response?.status !== 404) console.error('블로그 상태 확인 오류:', error.response?.data || error.message);
        this.hasBlog = false;
        this.blogUrl = '';
      }
    },
    // 알림 목록 가져오기
    async fetchNotifications() {
      if (!this.isLoggedIn) return;
      try {
        const fetchedNotifications = await authService.getNotifications();
        this.notifications = fetchedNotifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
      } catch (error) {
        console.error('알림 가져오기 실패:', error);
        this.notifications = [];
        this.unreadCount = 0;
      }
    },
    // 웹소켓 연결
    connectWebSocket() {
      if (!this.userId || !websocketService || !this.isLoggedIn) return;
      websocketService.disconnect();
      // eslint-disable-next-line no-unused-vars --- ESLint 오탐 수정
      websocketService.connect(this.userId, (notification) => {
        console.log('WebSocket notification received:', notification);
        if (!this.notifications.some(n => n.id === notification.id)) {
          this.notifications.unshift(notification);
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
          if (!notification.isRead) {
            this.unreadCount += 1;
          }
        }
      });
    },
    // 알림 클릭 처리
    async handleNotificationClick(notification) {
      if (!notification || !notification.id) {
        console.error("잘못된 알림 객체", notification);
        return;
      }
      try {
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          await this.fetchNotifications(); // 목록 갱신
        }
        if (notification.blogUrl && notification.menuId && notification.postId) {
          this.$router.push(`/${notification.blogUrl}/${notification.menuId}/${notification.postId}`);
        } else {
          console.warn("알림 클릭: 경로 정보 부족", notification);
        }
        this.closeDropdown();
      } catch (error) {
        console.error('알림 처리 실패:', error);
        alert('알림 처리 실패');
      }
    },
    // 날짜 포맷팅 유틸리티
    // eslint-disable-next-line no-unused-vars --- ESLint 오탐 수정
    formatDate(dateString) {
      if (!dateString) return '';
      try {
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR', {
          year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
      } catch (e) {
        console.error("날짜 포맷팅 오류:", e);
        return dateString;
      }
    },
    // 로그아웃 처리
    async handleLogout() {
      try {
        this.isLoggingOut = true;
        await authService.logout();
        this.resetAuthStates();
        this.$nextTick(() => {
          this.$router.push('/');
        });
      } catch (error) {
        console.error('로그아웃 실패:', error);
        this.resetAuthStates();
        alert('로그아웃 처리 중 오류');
      } finally {
        this.isLoggingOut = false;
        this.closeDropdown();
      }
    },
    // 검색 처리
    handleSearch() {
      const query = this.searchQuery.trim();
      if (query) {
        this.$router.push({path: '/search', query: {q: query}})
            .catch(err => {
              if (err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') console.error(err);
            });
      }
    },
    // --- 모달 관련 메소드 ---
    /** 페이지 로드 시 OAuth 프로필 완료 필요 여부 확인 */
    checkOAuthCompletionOnLoad() {
      const completionDataString = sessionStorage.getItem('oauth_profile_completion');
      if (completionDataString) {
        try {
          this.oauthCompletionDataForModal = JSON.parse(completionDataString);
          console.log("NavBar: OAuth Profile Completion 감지, 모달 자동 실행", this.oauthCompletionDataForModal);
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
    /** 로그인 버튼 클릭 시 모달 열기 */
    openLoginModal() {
      console.log("NavBar: 로그인 모달 열기");
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'login';
      this.showLoginModal = true;
    },
    /** 드롭다운 '닉네임 설정하기' 클릭 시 모달 열기 */
    openProfileCompletionModal() {
      console.log("NavBar: 프로필 완료 모달 열기 (드롭다운)");
      this.oauthCompletionDataForModal = {email: this.userEmail, oauthName: this.userName};
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.closeDropdown(); // 드롭다운 닫기
    },
    /** 모달 닫기 */
    closeLoginModal() {
      this.showLoginModal = false;
      this.oauthCompletionDataForModal = null;
    },
    /** 모달에서 인증 성공 시 콜백 */
    handleAuthSuccess() {
      console.log("NavBar: 모달 인증 성공. 상태 갱신.");
      this.closeLoginModal();
      // 약간의 딜레이 후 상태 갱신 (모달 닫히는 시간 고려)
      setTimeout(() => {
        this.checkLoginStatus();
      }, 100); // 100ms 딜레이 (조정 가능)
    }
  }
};
</script>

<style scoped>
/* NavBar 스타일은 이전 답변의 최종본과 동일하게 사용 */
.navbar {
  background-color: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 1000;
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
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 0.9rem;
  background-color: #f8f9fa;
  box-sizing: border-box;
  transition: border-color 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.search-bar input:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
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
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  padding: 2px 5px;
  font-size: 0.7rem;
  font-weight: 500;
  line-height: 1;
  min-width: 16px;
  text-align: center;
  box-shadow: 0 0 5px rgba(255, 0, 0, 0.5);
}
.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
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
  background-color: #f8f9fa;
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
  background-color: #eef6ff;
  font-weight: 500;
}

.notification-item.unread:hover {
  background-color: #e0f0ff;
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

/* 기본 색상 일치 */
button.dropdown-item.logout-btn {
  color: #d9534f;
}

/* 로그아웃만 빨간색 */
button.dropdown-item:hover:not(:disabled) {
  background-color: #f8f9fa;
}

button.dropdown-item:disabled {
  color: #aaa;
  cursor: not-allowed;
  background-color: transparent;
}
.login-button {
  color: #007bff;
  font-weight: 500;
  text-decoration: none;
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
  border: 1px solid #007bff;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out;
}

.login-button:hover {
  background-color: #007bff;
  color: white;
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

  /* .dropdown-menu { left: 50%; transform: translateX(-50%); right: auto; } */
}
</style>
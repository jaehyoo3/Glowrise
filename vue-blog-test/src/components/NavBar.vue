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
              <div v-if="notifications.length === 0 && unreadCount === 0" class="dropdown-item no-notifications">
                알림이 없습니다.
              </div>
              <div v-else>
                <div v-if="unreadCount > 0" class="dropdown-item mark-all-read-container">
                  <button class="mark-all-read-btn" @click="handleMarkAllRead">
                    전체 읽음
                  </button>
                </div>
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
import {mapActions, mapGetters, mapState} from 'vuex';
import authService from '@/services/authService';
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
      isLoggingOut: false,
      notifications: [],
      unreadCount: 0,
      showLoginModal: false,
      modalInitialTab: 'login',
      oauthCompletionDataForModal: null,
    };
  },
  computed: {
    ...mapState([
      // 'currentUser', 'userBlog' // 필요 시 직접 접근
    ]),
    ...mapGetters([
      'isLoggedIn',
      'userId',
      'username',
      'nickName',
      'userEmail',
      'userProfileImage',
      'blogUrl',
      'hasBlog',
      'isLoading'
    ]),
    displayProfileImage() {
      return this.userProfileImage || '/default-profile.png';
    },
    displayNicknameOrUsername() {
      return this.nickName || this.username;
    }
  },
  watch: {
    isLoggedIn(newValue, oldValue) {
      if (newValue) {
        this.fetchNotifications();
        this.connectWebSocket();
      } else {
        this.notifications = [];
        this.unreadCount = 0;
        if (websocketService) websocketService.disconnect();
      }
    }
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    if (websocketService) websocketService.disconnect();
  },
  created() {
    this.checkOAuthCompletionOnLoad();
    // 로그인 상태면 초기 알림 로드 및 웹소켓 연결
    if (this.isLoggedIn) {
      this.fetchNotifications();
      this.connectWebSocket();
    }
  },
  methods: {
    ...mapActions(['fetchCurrentUser', 'logoutAndClear']),

    handleOutsideClick(event) {
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
        if (this.isLoggedIn) {
          this.fetchNotifications(); // 드롭다운 열 때마다 최신 알림 가져오기
        }
      }
    },
    closeDropdown() {
      this.showUserDropdown = false;
      this.showNotificationDropdown = false;
    },
    async fetchNotifications() {
      if (!this.isLoggedIn) return;
      try {
        const fetchedNotifications = await authService.getNotifications();
        // isRead 필드를 boolean으로 변환 (API 응답이 문자열일 경우 대비)
        this.notifications = fetchedNotifications.map(n => ({
          ...n,
          isRead: typeof n.read === 'boolean' ? n.read : n.read === 'true'
        })).sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));

        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
      } catch (error) {
        console.error('NavBar: 알림 가져오기 실패:', error);
        this.notifications = [];
        this.unreadCount = 0;
      }
    },
    connectWebSocket() {
      if (!this.isLoggedIn || !this.userId) return;
      if (websocketService) websocketService.disconnect(); // 이전 연결 정리
      websocketService.connect(this.userId, (notification) => {
        if (!this.notifications.some(n => n.id === notification.id)) {
          const newNotification = {
            ...notification,
            isRead: typeof notification.read === 'boolean' ? notification.read : notification.read === 'true'
          };
          this.notifications.unshift(newNotification);
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
          if (!newNotification.isRead) {
            this.unreadCount += 1;
          }
        }
      });
    },
    async handleNotificationClick(notification) {
      if (!notification || !notification.id) return;
      try {
        // 읽지 않은 알림일 경우 읽음 처리 API 호출
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          // API 성공 후 즉시 UI 업데이트 및 목록 재조회
          notification.isRead = true; // 로컬 상태 즉시 변경
          this.unreadCount = Math.max(0, this.unreadCount - 1); // 안 읽은 카운트 감소
          // await this.fetchNotifications(); // 재조회 대신 로컬 업데이트로 UI 반응성 향상
        }

        // 관련 링크로 이동
        if (notification.blogUrl && notification.menuId && notification.postId) {
          const targetPath = `/${notification.blogUrl}/${notification.menuId}/${notification.postId}`;
          this.$router.push(targetPath).catch(() => {
          }); // 네비게이션 에러는 무시
        }
        this.closeDropdown();
      } catch (error) {
        console.error('NavBar: 알림 처리 실패:', error);
        alert('알림을 처리하는 중 오류가 발생했습니다.');
      }
    },

    async handleMarkAllRead() {
      if (this.unreadCount === 0) return; // 읽을 알림 없으면 중단

      try {
        await authService.markAllNotificationsAsRead();
        // 성공 시 로컬 상태 업데이트 (UI 즉시 반영)
        this.notifications.forEach(n => n.isRead = true);
        this.unreadCount = 0;
        // await this.fetchNotifications(); // 필요 시 서버와 동기화 위해 재조회
        this.closeDropdown(); // 드롭다운 닫기 (선택적)

      } catch (error) {
        console.error('NavBar: 전체 알림 읽음 처리 실패:', error);
        alert('전체 알림을 읽음 처리하는 중 오류가 발생했습니다.');
      }
    },

    formatDate(dateString) {
      if (!dateString) return '';
      try {
        const date = new Date(dateString);
        const now = new Date();
        const diffSeconds = Math.round((now - date) / 1000);
        const diffMinutes = Math.round(diffSeconds / 60);
        const diffHours = Math.round(diffMinutes / 60);
        const diffDays = Math.round(diffHours / 24);

        if (diffSeconds < 60) return `${diffSeconds}초 전`;
        if (diffMinutes < 60) return `${diffMinutes}분 전`;
        if (diffHours < 24) return `${diffHours}시간 전`;
        if (diffDays < 7) return `${diffDays}일 전`;
        return date.toLocaleDateString('ko-KR'); // 7일 이상이면 날짜 표시

      } catch (e) {
        return dateString;
      }
    },
    async handleLogout() {
      this.isLoggingOut = true;
      try {
        await this.logoutAndClear();
        window.dispatchEvent(new CustomEvent('auth-state-changed'));
        if (this.$route.path !== '/') {
          this.$router.push('/');
        }
      } catch (error) {
        alert('로그아웃 처리 중 오류가 발생했습니다.');
      } finally {
        this.isLoggingOut = false;
        this.closeDropdown();
      }
    },
    handleSearch() {
      const query = this.searchQuery.trim();
      if (query) {
        this.$router.push({path: '/search', query: {q: query}}).catch(() => {
        });
        this.searchQuery = '';
      }
    },
    checkOAuthCompletionOnLoad() {
      const completionDataString = sessionStorage.getItem('oauth_profile_completion');
      if (completionDataString) {
        try {
          this.oauthCompletionDataForModal = JSON.parse(completionDataString);
          this.modalInitialTab = 'signup';
          this.showLoginModal = true;
          sessionStorage.removeItem('oauth_profile_completion');
        } catch (e) {
          sessionStorage.removeItem('oauth_profile_completion');
          this.oauthCompletionDataForModal = null;
        }
      } else {
        this.oauthCompletionDataForModal = null;
      }
    },
    openLoginModal() {
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'login';
      this.showLoginModal = true;
    },
    openSignupModal() {
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
    },
    openProfileCompletionModal() {
      this.oauthCompletionDataForModal = {
        email: this.userEmail,
        oauthName: this.username
      };
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.closeDropdown();
    },
    closeLoginModal() {
      this.showLoginModal = false;
      this.oauthCompletionDataForModal = null;
    },
    async handleAuthSuccess() {
      this.closeLoginModal();
      try {
        await this.fetchCurrentUser();
        window.dispatchEvent(new CustomEvent('auth-state-changed'));
      } catch (error) {
        console.error("NavBar: handleAuthSuccess 내 fetchCurrentUser 중 에러 발생:", error);
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

.dropdown-item:hover:not(.mark-all-read-container) { /* 전체 읽음 컨테이너 호버 제외 */
  background-color: #f5f5f5;
}

.mark-all-read-container {
  padding: 0.5rem 1.2rem; /* 버튼 주위 여백 조정 */
  border-bottom: 1px solid #eee; /* 구분선 */
}

.mark-all-read-btn {
  background-color: #eee;
  color: #333;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 500;
  width: 100%;
  text-align: center;
  transition: background-color 0.2s ease;
}

.mark-all-read-btn:hover {
  background-color: #ddd;
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
  font-weight: 500;
}

.notification-item.unread::before {
  content: '●';
  color: #007bff;
  font-size: 0.7em;
  margin-right: 8px;
  vertical-align: middle;
}

.notification-item:hover {
  background-color: #f5f5f5;
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
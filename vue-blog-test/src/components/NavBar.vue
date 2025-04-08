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
        @profile-completed="handleAuthSuccess"
    />
  </div>
</template>

<script>
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
      isLoggedIn: false,
      userId: null,
      userName: '',
      nickName: '',
      userEmail: '',
      userProfileImage: '',
      isLoggingOut: false,
      hasBlog: false,
      blogUrl: '',
      notifications: [],
      unreadCount: 0,
      showLoginModal: false,
      modalInitialTab: 'login',
      oauthCompletionDataForModal: null,
    };
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
    this.checkOAuthCompletionOnLoad();
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    if (websocketService) websocketService.disconnect();
  },
  created() {
    this.checkLoginStatus();
  },
  methods: {
    handleOutsideClick(event) {
      const userMenu = this.$el.querySelector('.user-menu');
      const notificationMenu = this.$el.querySelector('.notification-menu');
      if (userMenu && !userMenu.contains(event.target) &&
          notificationMenu && !notificationMenu.contains(event.target)) {
        this.showUserDropdown = false;
        this.showNotificationDropdown = false;
      }
    },
    toggleUserDropdown() {
      this.showUserDropdown = !this.showUserDropdown;
      if (this.showUserDropdown) this.showNotificationDropdown = false;
    },
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      if (this.showNotificationDropdown) {
        this.showUserDropdown = false;
        this.fetchNotifications();
      }
    },
    closeDropdown() {
      this.showUserDropdown = false;
      this.showNotificationDropdown = false;
    },
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
    connectWebSocket() {
      if (!this.userId || !websocketService || !this.isLoggedIn) return;
      websocketService.disconnect();
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
    async handleNotificationClick(notification) {
      if (!notification || !notification.id) {
        console.error("잘못된 알림 객체", notification);
        return;
      }
      try {
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          await this.fetchNotifications();
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
    handleSearch() {
      const query = this.searchQuery.trim();
      if (query) {
        this.$router.push({path: '/search', query: {q: query}})
            .catch(err => {
              if (err.name !== 'NavigationDuplicated' && err.name !== 'NavigationCancelled') console.error(err);
            });
      }
    },
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
    openLoginModal() {
      console.log("NavBar: 로그인 모달 열기");
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'login';
      this.showLoginModal = true;
    },
    openSignupModal() {
      console.log("NavBar: 회원가입 모달 열기");
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
    },
    openProfileCompletionModal() {
      console.log("NavBar: 프로필 완료 모달 열기 (드롭다운)");
      this.oauthCompletionDataForModal = {email: this.userEmail, oauthName: this.userName};
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.closeDropdown();
    },
    closeLoginModal() {
      this.showLoginModal = false;
      this.oauthCompletionDataForModal = null;
    },
    handleAuthSuccess() {
      console.log("NavBar: 모달 인증 성공. 상태 갱신.");
      this.closeLoginModal();
      setTimeout(() => {
        this.checkLoginStatus();
      }, 100);
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
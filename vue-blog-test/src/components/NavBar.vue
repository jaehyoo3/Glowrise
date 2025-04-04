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
        >
        <button class="search-button" @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>
      </div>

      <div class="navbar-menu">
        <template v-if="isLoggedIn">
          <!-- 알림 아이콘 및 드롭다운 -->
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

          <!-- 사용자 메뉴 -->
          <div class="user-menu" @click.stop="toggleUserDropdown">
            <span class="user-name">{{ userName }}</span>
            <i class="fas fa-chevron-down"></i>

            <div v-if="showUserDropdown" class="dropdown-menu" @click.stop>
              <template v-if="hasBlog">
                <router-link
                    :to="`/${blogUrl}`"
                    class="dropdown-item"
                    @click="closeDropdown"
                >
                  내 블로그
                </router-link>
                <router-link
                    to="/profile/edit"
                    class="dropdown-item"
                    @click="closeDropdown"
                >
                  내 정보 수정
                </router-link>
              </template>
              <template v-else>
                <router-link
                    to="/blog/create"
                    class="dropdown-item"
                    @click="closeDropdown"
                >
                  블로그 만들기
                </router-link>
              </template>
              <div class="dropdown-divider"></div>
              <button
                  @click="handleLogout"
                  class="dropdown-item"
                  :disabled="isLoggingOut"
              >
                {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
              </button>
            </div>
          </div>
        </template>

        <template v-else>
          <router-link to="/login" class="login-button">로그인</router-link>
        </template>
      </div>
    </div>
  </div>
</template>
<script>
import authService from '@/services/authService';
import {websocketService} from '@/services/websocketService';

export default {
  name: 'NavBar',
  data() {
    return {
      searchQuery: '',
      showUserDropdown: false,
      showNotificationDropdown: false,
      isLoggedIn: false,
      userName: '',
      userProfileImage: '',
      isLoggingOut: false,
      hasBlog: false,
      userId: null,
      blogUrl: '',
      notifications: [], // 알림 목록
      unreadCount: 0, // 읽지 않은 알림 개수
    };
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
    websocketService.disconnect();
  },
  created() {
    this.checkLoginStatus();
  },
  methods: {
    handleOutsideClick(event) {
      const userMenu = this.$el.querySelector('.user-menu');
      const notificationMenu = this.$el.querySelector('.notification-menu');
      if (
          (userMenu && !userMenu.contains(event.target)) &&
          (notificationMenu && !notificationMenu.contains(event.target))
      ) {
        this.showUserDropdown = false;
        this.showNotificationDropdown = false;
      }
    },
    toggleUserDropdown() {
      console.log('Toggling user dropdown, current state:', this.showUserDropdown);
      this.showUserDropdown = !this.showUserDropdown;
      this.showNotificationDropdown = false; // 다른 드롭다운 닫기
    },
    toggleNotificationDropdown() {
      console.log('Toggling notification dropdown, current state:', this.showNotificationDropdown);
      this.showNotificationDropdown = !this.showNotificationDropdown;
      this.showUserDropdown = false; // 다른 드롭다운 닫기
      if (this.showNotificationDropdown) {
        this.fetchNotifications();
      }
    },
    closeDropdown() {
      this.showUserDropdown = false;
      this.showNotificationDropdown = false;
    },
    async checkLoginStatus() {
      const user = authService.getStoredUser();
      if (user) {
        this.isLoggedIn = true;
        this.userName = user.username;
        this.userId = user.id;
        this.userProfileImage = user.profileImage || '';
        await this.checkBlogStatus();
        await this.fetchNotifications(); // 알림 목록 초기 로드
        this.connectWebSocket(); // WebSocket 연결
      } else {
        this.isLoggedIn = false;
        this.userName = '';
        this.userId = null;
        this.userProfileImage = '';
        this.hasBlog = false;
        this.blogUrl = '';
        this.notifications = [];
        this.unreadCount = 0;
        try {
          const serverUser = await authService.getCurrentUser();
          authService.setStoredUser({id: serverUser.userId || serverUser.id, username: serverUser.username});
          this.checkLoginStatus();
        } catch (error) {
          console.error('현재 사용자 정보 가져오기 실패:', error);
        }
      }
    },
    async checkBlogStatus() {
      if (!this.userId) {
        this.hasBlog = false;
        this.blogUrl = '';
        return;
      }
      try {
        const response = await authService.getBlogByUserId();
        this.hasBlog = response !== null && response.id !== undefined;
        this.blogUrl = response?.url || '';
      } catch (error) {
        console.error('블로그 상태 확인 실패:', error);
        this.hasBlog = false;
        this.blogUrl = '';
      }
    },
    async fetchNotifications() {
      if (!this.isLoggedIn) return;
      try {
        console.log('Fetching notifications...');
        const newNotifications = await authService.getNotifications();
        // 중복 제거 및 병합
        const existingIds = new Set(this.notifications.map(n => n.id));
        this.notifications = [
          ...this.notifications,
          ...newNotifications.filter(n => !existingIds.has(n.id))
        ];
        this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
      } catch (error) {
        console.error('알림 목록 가져오기 실패:', error);
        this.notifications = [];
        this.unreadCount = 0;
      }
    },
    connectWebSocket() {
      if (!this.userId) return;
      websocketService.connect(this.userId, (notification) => {
        console.log('New notification received:', notification);
        // 중복 알림 방지
        if (!this.notifications.some(n => n.id === notification.id)) {
          this.notifications.unshift(notification);
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
          if (!notification.isRead) {
            this.unreadCount += 1; // 읽지 않은 알림이면 카운트 증가
          }
        }
      });
    },
    async handleNotificationClick(notification) {
      try {
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          notification.isRead = true;
          this.unreadCount = this.notifications.filter(n => !n.isRead).length;
        }
        this.$router.push(`/${notification.blogUrl}/${notification.menuId}/${notification.postId}`);
        this.closeDropdown();
      } catch (error) {
        console.error('알림 읽음 처리 실패:', error);
        alert('알림을 처리하는 데 실패했습니다.');
      }
    },
    formatDate(date) {
      return new Date(date).toLocaleString();
    },
    async handleLogout() {
      try {
        this.isLoggingOut = true;
        await authService.logout();
        this.isLoggedIn = false;
        this.userName = '';
        this.userId = null;
        this.userProfileImage = '';
        this.hasBlog = false;
        this.blogUrl = '';
        this.notifications = [];
        this.unreadCount = 0;
        websocketService.disconnect(); // WebSocket 연결 해제
        this.$router.push('/login');
        alert('로그아웃되었습니다.');
      } catch (error) {
        console.error('로그아웃 중 오류 발생:', error);
        alert('로그아웃에 실패했습니다. 다시 시도해주세요.');
      } finally {
        this.isLoggingOut = false;
        this.showUserDropdown = false;
      }
    },
    handleSearch() {
      if (this.searchQuery.trim()) {
        this.$router.push({
          path: '/search',
          query: { q: this.searchQuery.trim() }
        });
      }
    },
  },
};
</script>

<style scoped>
.navbar {
  background-color: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
}

.navbar-brand .logo {
  font-size: 1.75rem;
  font-weight: 700;
  color: #000;
  text-decoration: none;
}

.search-bar {
  flex: 1;
  max-width: 400px;
  position: relative;
  margin: 0 2rem;
}

.search-bar input {
  width: 100%;
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 0.9rem;
  background-color: #f8f9fa;
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
}

.navbar-menu {
  display: flex;
  align-items: center;
}

/* 알림 메뉴 스타일 */
.notification-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  margin-right: 1rem;
  z-index: 2001;
}

.notification-menu i {
  font-size: 1.2rem;
  color: #333;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 0.7rem;
  font-weight: 500;
}

.notification-dropdown {
  max-height: 300px;
  overflow-y: auto;
  width: 250px;
}

.notification-item {
  display: flex;
  flex-direction: column;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e5e5;
  cursor: pointer;
}

.notification-item.unread {
  background-color: #f0f8ff;
  font-weight: 500;
}

.notification-item small {
  color: #666;
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.no-notifications {
  padding: 0.75rem 1rem;
  color: #666;
  text-align: center;
}

/* 사용자 메뉴 스타일 */
.user-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  margin-right: 1rem;
  z-index: 2001;
}

.user-name {
  font-size: 0.9rem;
  font-weight: 500;
  margin-right: 0.5rem;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: white;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 160px;
  padding: 0.5rem 0;
  z-index: 2000;
  display: block;
}

.dropdown-item {
  display: block;
  padding: 0.5rem 1rem;
  color: #333;
  text-decoration: none;
  font-size: 0.9rem;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f8f9fa;
}

.dropdown-divider {
  height: 1px;
  background-color: #e5e5e5;
  margin: 0.5rem 0;
}

.write-button {
  background-color: #000;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  font-weight: 500;
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: background-color 0.2s;
}

.write-button:hover {
  background-color: #333;
}

.write-button i {
  margin-right: 0.5rem;
}

.login-button {
  color: #000;
  font-weight: 500;
  text-decoration: none;
  font-size: 0.9rem;
  padding: 0.5rem 1rem;
  border: 1px solid #000;
  border-radius: 4px;
  transition: background-color 0.2s, color 0.2s;
}

.login-button:hover {
  background-color: #000;
  color: white;
}

@media (max-width: 768px) {
  .container {
    flex-wrap: wrap;
  }

  .navbar-brand {
    margin-bottom: 0.5rem;
  }

  .search-bar {
    order: 3;
    max-width: 100%;
    margin: 0.5rem 0 0;
  }
}
</style>
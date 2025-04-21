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
            @keyup.enter="handleSearch"/>
        <button class="search-button" @click="handleSearch">
          <i class="pi pi-search"></i>
        </button>
      </div>

      <div class="navbar-menu">
        <template v-if="isLoggedIn">
          <div class="notification-menu" @click.stop="toggleNotificationDropdown">
            <button class="notification-button">
              <i class="pi pi-bell"></i>
              <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount }}</span>
            </button>

            <div v-if="showNotificationDropdown" class="dropdown-menu notification-dropdown" @click.stop>
              <div class="notification-header">
                <h3>알림</h3>
                <button v-if="unreadCount > 0" class="mark-all-read-btn" @click="handleMarkAllRead">
                  전체 읽음
                </button>
              </div>

              <div class="notification-content">
                <div v-if="notifications.length === 0" class="empty-notifications">
                  <i class="pi pi-inbox"></i>
                  <p>알림이 없습니다</p>
                </div>

                <div
                    v-for="notification in notifications"
                    :key="notification.id"
                    class="notification-item"
                    :class="{ 'unread': !notification.isRead }"
                    @click="handleNotificationClick(notification)"
                >
                  <div v-if="!notification.isRead" class="notification-indicator"></div>
                  <div class="notification-text">
                    <p>{{ notification.message }}</p>
                    <span class="notification-time">{{ formatDate(notification.createdDate) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="user-menu">
            <button class="user-menu-button" @click="toggleUserMenu">
              <div class="user-avatar">
                <img :alt="displayNicknameOrUsername" :src="displayProfileImage"/>
              </div>
              <span class="username">{{ displayNicknameOrUsername }}</span>
              <i class="pi pi-chevron-down"></i>
            </button>

            <div v-if="showUserDropdown" class="dropdown-menu user-dropdown">
              <div class="user-info">
                <div class="user-avatar-large">
                  <img :alt="displayNicknameOrUsername" :src="displayProfileImage"/>
                </div>
                <div class="user-details">
                  <h3>{{ displayNicknameOrUsername }}</h3>
                  <p>{{ userEmail }}</p>
                </div>
              </div>

              <div class="menu-items">
                <router-link v-if="hasBlog" :to="`/${blogUrl}`" class="menu-item">
                  <i class="pi pi-home"></i>
                  <span>내 블로그</span>
                </router-link>

                <router-link v-else-if="nickName" class="menu-item" to="/blog/create">
                  <i class="pi pi-plus"></i>
                  <span>블로그 만들기</span>
                </router-link>

                <button v-else class="menu-item" @click="openProfileCompletionModal">
                  <i class="pi pi-user-plus"></i>
                  <span>닉네임 설정하기</span>
                </button>

                <router-link class="menu-item" to="/profile/edit">
                  <i class="pi pi-user-edit"></i>
                  <span>내 정보 수정</span>
                </router-link>

                <template v-if="isAdmin">
                  <div class="menu-divider"></div>
                  <div class="menu-group">관리자 메뉴</div>

                  <button class="menu-item" @click="openAdminModal">
                    <i class="pi pi-cog"></i>
                    <span>관리자 설정</span>
                  </button>
                </template>

                <div class="menu-divider"></div>

                <button class="menu-item logout" @click="handleLogout">
                  <i class="pi pi-power-off"></i>
                  <span>로그아웃</span>
                </button>
              </div>
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

    <AdminManagementModal v-model:visible="isAdminModalVisible"/>
  </div>
</template>

<script>
// Vuex 헬퍼 함수 임포트
import {mapActions, mapGetters} from 'vuex';
// 서비스 임포트
import authService from '@/services/authService';
import {websocketService} from '@/services/websocketService';
// 자식 컴포넌트 임포트
import LoginSignupModal from '@/components/LoginSignupModal.vue';
// import AdvertisementModal from '@/components/admin/AdvertisementModal.vue'; // NavBar에서 직접 사용 안 하므로 제거
import AdminManagementModal from '@/components/admin/AdminManagementModal.vue'; // 관리자 모달 임포트

export default {
  name: 'NavBar',
  components: {
    LoginSignupModal,
    // AdvertisementModal, // 컴포넌트 등록 제거
    AdminManagementModal // 관리자 모달 컴포넌트 등록
  },
  data() {
    return {
      searchQuery: '',
      showNotificationDropdown: false,
      showUserDropdown: false,
      isLoggingOut: false,
      notifications: [],
      unreadCount: 0,
      showLoginModal: false,
      modalInitialTab: 'login',
      oauthCompletionDataForModal: null,
      // isAdModalVisible: false, // 기존 광고 모달 상태 제거
      isAdminModalVisible: false, // 관리자 모달 표시 상태 추가
    };
  },
  computed: {
    // mapState 사용 시 필요한 상태 명시 (현재 코드에서는 직접 사용 안 함)
    // ...mapState([]),

    // mapGetters 로 Vuex 상태/게터 가져오기
    ...mapGetters([
      'isLoggedIn', 'userId', 'username', 'nickName', 'userEmail',
      'userProfileImage', 'blogUrl', 'hasBlog', 'isLoading', 'isAdmin' // isAdmin 게터 사용
    ]),

    // 프로필 이미지 경로 계산
    displayProfileImage() {
      return this.userProfileImage || '/img/default-profile.png';
    },

    // 표시할 이름 계산 (닉네임 우선)
    displayNicknameOrUsername() {
      return this.nickName || this.username || '사용자';
    },

    // 사용자 드롭다운 메뉴 아이템 계산
    userMenuItems() {
      let items = [];
      if (this.isLoggedIn) {
        items = [
          {
            label: '마이페이지', // 실제 기능에 맞게 수정
            icon: 'pi pi-user-edit',
            command: () => {
              // 마이페이지 또는 프로필 수정 페이지로 이동
              // 예: this.$router.push('/profile/edit');
              alert('마이페이지 기능 구현 필요');
              this.showUserDropdown = false;
            }
          },
          {
            label: '내 블로그',
            icon: 'pi pi-book',
            visible: this.hasBlog, // 블로그가 있을 때만 표시
            command: () => {
              if (this.blogUrl) {
                this.$router.push(`/${this.blogUrl}`);
              }
              this.showUserDropdown = false;
            }
          },
          {
            label: '블로그 관리',
            icon: 'pi pi-cog',
            visible: this.hasBlog, // 블로그가 있을 때만 표시
            command: () => {
              if (this.blogId) {
                this.$router.push(`/blog/edit/${this.blogId}`);
              } else {
                // 블로그 ID 없는 경우 예외 처리 또는 메시지
              }
              this.showUserDropdown = false;
            }
          },
          {
            separator: true
          },
          {
            label: '로그아웃',
            icon: 'pi pi-sign-out',
            command: this.handleLogout // 로그아웃 메소드 호출
          }
        ];

        // 관리자일 경우 '관리자 설정' 메뉴 추가
        if (this.isAdmin) {
          // 구분선 앞에 추가 (순서 조정)
          items.splice(items.length - 2, 0, {
            label: '관리자 설정', // 이름 변경
            icon: 'pi pi-cog',
            command: () => {
              this.openAdminModal(); // 관리자 모달 열기
              this.showUserDropdown = false;
            }
          });
        }
      }
      return items;
    }
  },
  watch: {
    isLoggedIn(newValue) {
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
    if (this.isLoggedIn) {
      this.fetchNotifications();
      this.connectWebSocket();
    }
  },
  methods: {
    ...mapActions(['fetchCurrentUser', 'logoutAndClear']),

    handleOutsideClick(event) {
      const notificationMenu = this.$el.querySelector('.notification-menu');
      if (notificationMenu && !notificationMenu.contains(event.target)) {
        this.showNotificationDropdown = false;
      }
      const userMenu = this.$el.querySelector('.user-menu');
      if (userMenu && !userMenu.contains(event.target)) {
        this.showUserDropdown = false;
      }
    },

    toggleUserMenu() {
      this.showUserDropdown = !this.showUserDropdown;
      this.showNotificationDropdown = false;
    },

    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      this.showUserDropdown = false;
      if (this.showNotificationDropdown && this.isLoggedIn) {
        this.fetchNotifications();
      }
    },

    async fetchNotifications() {
      if (!this.isLoggedIn) return;
      try {
        const fetchedNotifications = await authService.getNotifications();
        this.notifications = fetchedNotifications.map(n => ({
          ...n,
          isRead: typeof n.read === 'boolean' ? n.read : String(n.read).toLowerCase() === 'true'
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
      if (websocketService) websocketService.disconnect();
      websocketService.connect(this.userId, (notification) => {
        if (!this.notifications.some(n => n.id === notification.id)) {
          const newNotification = {
            ...notification,
            isRead: typeof notification.read === 'boolean' ? notification.read : String(notification.read).toLowerCase() === 'true'
          };
          this.notifications.unshift(newNotification);
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate));
          if (!newNotification.isRead) {
            this.unreadCount += 1;
          }
          this.$toast.add({severity: 'info', summary: '새 알림', detail: notification.message, life: 5000});
        }
      });
    },

    async handleNotificationClick(notification) {
      if (!notification || !notification.id) return;
      try {
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          const targetNotification = this.notifications.find(n => n.id === notification.id);
          if (targetNotification) {
            targetNotification.isRead = true;
          }
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        }
        if (notification.blogUrl && notification.menuId && notification.postId) {
          const targetPath = `/${notification.blogUrl}/${notification.menuId}/${notification.postId}`;
          this.$router.push(targetPath).catch(() => {
          });
        }
        this.showNotificationDropdown = false;
      } catch (error) {
        console.error('NavBar: 알림 처리 실패:', error);
        this.$toast.add({severity: 'error', summary: '오류', detail: '알림 처리 중 오류', life: 3000});
      }
    },

    async handleMarkAllRead() {
      if (this.unreadCount === 0) return;
      try {
        await authService.markAllNotificationsAsRead();
        this.notifications.forEach(n => n.isRead = true);
        this.unreadCount = 0;
        this.$toast.add({severity: 'success', summary: '알림', detail: '모든 알림 읽음 처리 완료', life: 3000});
      } catch (error) {
        console.error('NavBar: 전체 알림 읽음 처리 실패:', error);
        this.$toast.add({severity: 'error', summary: '오류', detail: '전체 알림 읽음 처리 중 오류', life: 3000});
      }
    },

    formatDate(dateString) {
      // ... (기존 formatDate 로직 유지) ...
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
        return date.toLocaleDateString('ko-KR');
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
          this.$router.push('/').catch(() => {
          });
        }
        this.$toast.add({severity: 'info', summary: '로그아웃', detail: '로그아웃 완료', life: 3000});
      } catch (error) {
        console.error("NavBar: 로그아웃 오류", error);
        this.$toast.add({severity: 'error', summary: '오류', detail: '로그아웃 처리 중 오류', life: 3000});
      } finally {
        this.isLoggingOut = false;
        this.showUserDropdown = false; // 드롭다운 닫기
      }
    },

    handleSearch() {
      const query = this.searchQuery.trim();
      if (query) {
        this.$router.push({name: 'search', query: {q: query}})
            .catch(err => {
              if (err.name !== 'NavigationDuplicated' && !err.message.includes('Avoided redundant navigation')) {
                console.error('Search navigation error:', err);
              }
            });
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
      this.showUserDropdown = false; // 드롭다운 닫기
    },

    openSignupModal() {
      this.oauthCompletionDataForModal = null;
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.showUserDropdown = false; // 드롭다운 닫기
    },

    openProfileCompletionModal() {
      this.oauthCompletionDataForModal = {email: this.userEmail, oauthName: this.username};
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
      this.showUserDropdown = false; // 드롭다운 닫기
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
        console.error("NavBar: 인증 성공 후 사용자 정보 로드 오류:", error);
      }
    },

    // 관리자 모달 열기 메소드 추가
    openAdminModal() {
      this.isAdminModalVisible = true;
      this.showUserDropdown = false; // 드롭다운 닫기
    },

    // 기존 광고 생성 모달 열기 메소드 제거
    // openCreateAdModal() {
    //   this.isAdModalVisible = true;
    // },

    // 기존 광고 저장 완료 핸들러 제거
    // handleAdSaved() {
    //   this.$toast.add({severity: 'success', summary: '성공', detail: '광고 생성 완료', life: 3000});
    // }
  }
};
</script>

<style>
/* --- 전체 네비게이션 스타일 --- */
.navbar {
  background-color: white;
  border-bottom: 1px solid #f0f0f0;
  padding: 0.8rem 0;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
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

/* --- 로고 스타일 --- */
.navbar-brand .logo {
  font-size: 1.6rem;
  font-weight: 600;
  color: #333;
  text-decoration: none;
  letter-spacing: -0.3px;
  background: black;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  padding: 0.2rem 0;
}

/* --- 검색창 스타일 --- */
.search-bar {
  flex-grow: 1;
  max-width: 400px;
  position: relative;
  margin: 0 2rem;
}

.search-bar input {
  width: 100%;
  padding: 0.65rem 2.5rem 0.65rem 1.2rem;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  background-color: #f5f7fa;
  box-sizing: border-box;
  transition: all 0.2s ease-in-out;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.search-bar input:focus {
  background-color: #fff;
  box-shadow: 0 0 0 2px rgba(49, 130, 206, 0.2), inset 0 0 0 1px rgba(0, 0, 0, 0.05);
  outline: none;
}

.search-button {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  color: #888;
  cursor: pointer;
  padding: 5px;
  transition: color 0.2s;
}

.search-button:hover {
  color: #3182ce;
}

/* --- 메뉴 영역 --- */
.navbar-menu {
  display: flex;
  align-items: center;
}

/* --- 알림 메뉴 스타일 --- */
.notification-menu {
  position: relative;
  margin-right: 1.2rem;
  z-index: 1001;
}

.notification-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: #f5f7fa;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
}

.notification-button:hover {
  background-color: #e6f0ff;
}

.notification-button i {
  color: #4a5568;
  font-size: 1.1rem;
}

.notification-badge {
  position: absolute;
  top: 0px;
  right: 0px;
  background-color: #e53e3e;
  color: white;
  border-radius: 50%;
  font-size: 0.75rem;
  font-weight: 600;
  height: 18px;
  width: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
}

/* --- 알림 드롭다운 --- */
.notification-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: -100px;
  width: 320px;
  max-height: 480px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 1000;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.notification-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #2d3748;
}

.mark-all-read-btn {
  background: none;
  border: none;
  color: #3182ce;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.mark-all-read-btn:hover {
  color: #2b6cb0;
  text-decoration: underline;
}

.notification-content {
  max-height: 400px;
  overflow-y: auto;
}

.notification-content::-webkit-scrollbar {
  width: 6px;
}

.notification-content::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
}

.empty-notifications {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #a0aec0;
}

.empty-notifications i {
  font-size: 2rem;
  margin-bottom: 12px;
}

.empty-notifications p {
  font-size: 0.9rem;
  margin: 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background-color: #f7fafc;
}

.notification-indicator {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3182ce;
  margin-top: 6px;
  margin-right: 12px;
}

.notification-text {
  flex-grow: 1;
}

.notification-text p {
  margin: 0 0 4px 0;
  font-size: 0.9rem;
  color: #2d3748;
  line-height: 1.5;
}

.notification-time {
  font-size: 0.75rem;
  color: #a0aec0;
}

.notification-item.unread {
  background-color: #ebf8ff;
}

.notification-item.unread:hover {
  background-color: #e6f0ff;
}

/* --- 사용자 메뉴 스타일 --- */
.user-menu {
  position: relative;
  z-index: 1001;
}

.user-menu-button {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background-color: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-menu-button:hover {
  background-color: #f5f7fa;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 8px;
  border: 2px solid #e2e8f0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.username {
  font-size: 0.9rem;
  font-weight: 500;
  color: #2d3748;
  margin-right: 4px;
}

.user-menu-button i {
  color: #a0aec0;
  font-size: 0.75rem;
}

/* --- 사용자 드롭다운 --- */
.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  z-index: 1000;
}

.user-info {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #f8fafc;
}

.user-avatar-large {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  border: 2px solid #e2e8f0;
}

.user-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-details h3 {
  margin: 0 0 4px 0;
  font-size: 1rem;
  font-weight: 600;
  color: #2d3748;
}

.user-details p {
  margin: 0;
  font-size: 0.85rem;
  color: #718096;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-items {
  padding: 8px 0;
}

.menu-item {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  text-align: left;
  font-size: 0.9rem;
  color: #4a5568;
  cursor: pointer;
  transition: background-color 0.15s ease;
  text-decoration: none;
}

.menu-item:hover {
  background-color: #f7fafc;
}

.menu-item i {
  width: 20px;
  margin-right: 12px;
  color: #718096;
}

.menu-divider {
  height: 1px;
  background-color: #f0f0f0;
  margin: 8px 0;
}

.menu-group {
  padding: 6px 16px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #a0aec0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.menu-item.logout {
  color: #e53e3e;
}

.menu-item.logout i {
  color: #e53e3e;
}

/* --- 로그인/회원가입 버튼 --- */
.auth-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.login-button, .signup-button {
  padding: 8px 16px;
  font-size: 0.9rem;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-button {
  background-color: transparent;
  color: #4a5568;
  border: 1px solid #e2e8f0;
}

.login-button:hover {
  background-color: #f5f7fa;
  border-color: #cbd5e0;
}

.signup-button {
  background-color: #3182ce;
  color: white;
  border: none;
}

.signup-button:hover {
  background-color: #2b6cb0;
}

/* --- 반응형 스타일 --- */
@media (max-width: 768px) {
  .container {
    flex-wrap: wrap;
    justify-content: space-between;
    padding: 0 1rem;
  }

  .navbar-brand {
    width: auto;
    text-align: left;
    margin-bottom: 0;
  }

  .search-bar {
    order: 3;
    max-width: 100%;
    margin: 0.8rem 0 0;
  }

  .navbar-menu {
    order: 2;
    width: auto;
    justify-content: flex-end;
  }

  .notification-dropdown {
    width: 90vw;
    max-width: 320px;
    right: -80px;
  }

  .user-dropdown {
    width: 260px;
    right: -60px;
  }

  .username {
    display: none;
  }

  .user-menu-button i {
    display: none;
  }
}

@media (max-width: 480px) {
  .navbar {
    padding: 0.6rem 0;
  }

  .container {
    padding: 0 0.8rem;
  }

  .navbar-brand .logo {
    font-size: 1.4rem;
  }

  .notification-button {
    width: 36px;
    height: 36px;
  }

  .user-avatar {
    width: 28px;
    height: 28px;
    margin-right: 0;
  }

  .auth-buttons {
    gap: 8px;
  }

  .login-button, .signup-button {
    padding: 6px 12px;
    font-size: 0.85rem;
  }
}
</style>
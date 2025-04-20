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
        <button class="search-button" @click="handleSearch"><i class="fas fa-search"></i></button>
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
                  <span>{{ notification.message }}</span> <small>{{ formatDate(notification.createdDate) }}</small>
                </div>
              </div>
            </div>
          </div>
          <div class="user-menu">
            <PrimeButton
                :label="displayNicknameOrUsername"
                aria-controls="overlay_menu"
                aria-haspopup="true" class="p-button-rounded p-button-text p-button-plain user-menu-button"
                icon="pi pi-user"
                type="button"
                @click="toggleUserMenu"
            />
            <PrimeMenu id="overlay_menu" ref="userMenuRef" :model="userMenuItems" :popup="true"/>
          </div>
        </template>
        <template v-else>
          <div class="auth-buttons">
            <PrimeButton class="signup-button p-button-sm" label="회원가입" @click="openSignupModal"/>
            <PrimeButton class="login-button p-button-sm" label="로그인" @click="openLoginModal"/>
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

    <AdvertisementModal v-model:visible="isAdModalVisible" @saved="handleAdSaved"/>
  </div>
</template>

<script>
// Vuex 헬퍼 함수 임포트
import {mapActions, mapGetters, mapState} from 'vuex';
// 서비스 임포트
import authService from '@/services/authService';
import {websocketService} from '@/services/websocketService';
// 자식 컴포넌트 임포트
import LoginSignupModal from '@/components/LoginSignupModal.vue';
import AdvertisementModal from '@/components/admin/AdvertisementModal.vue';

// PrimeVue 컴포넌트는 main.js에서 전역 등록됨을 가정

export default {
  name: 'NavBar',
  components: {
    LoginSignupModal,
    AdvertisementModal
  },
  data() {
    return {
      searchQuery: '', // 검색어 바인딩
      showNotificationDropdown: false,
      isLoggingOut: false,
      notifications: [],
      unreadCount: 0,
      showLoginModal: false,
      modalInitialTab: 'login',
      oauthCompletionDataForModal: null,
      isAdModalVisible: false,
    };
  },
  computed: {
    // Vuex 상태 및 게터 매핑
    ...mapState([]), // 필요시 상태 추가
    ...mapGetters([
      'isLoggedIn', 'userId', 'username', 'nickName', 'userEmail',
      'userProfileImage', 'blogUrl', 'hasBlog', 'isLoading', 'isAdmin'
    ]),
    displayProfileImage() {
      return this.userProfileImage || '/img/default-profile.png';
    },
    displayNicknameOrUsername() {
      return this.nickName || this.username || '사용자';
    },
    userMenuItems() {
      let items = [];
      if (this.hasBlog) {
        items.push({
          label: '내 블로그',
          icon: 'pi pi-fw pi-home',
          command: () => this.$router.push(`/${this.blogUrl}`).catch(() => {
          })
        });
      } else {
        if (this.nickName) {
          items.push({
            label: '블로그 만들기',
            icon: 'pi pi-fw pi-plus',
            command: () => this.$router.push('/blog/create').catch(() => {
            })
          });
        } else {
          items.push({
            label: '닉네임 설정하기',
            icon: 'pi pi-fw pi-user-plus',
            command: () => this.openProfileCompletionModal()
          });
        }
      }
      items.push({
        label: '내 정보 수정',
        icon: 'pi pi-fw pi-user-edit',
        command: () => this.$router.push('/profile/edit').catch(() => {
        })
      });
      if (this.isAdmin) {
        items.push({separator: true}, {
          label: '관리자 메뉴', icon: 'pi pi-fw pi-cog',
          items: [
            {
              label: '사용자 관리', icon: 'pi pi-fw pi-users', command: () => this.$router.push('/admin/users').catch(() => {
              })
            }, // 경로 확인
            {label: '광고 생성', icon: 'pi pi-fw pi-plus-circle', command: () => this.openCreateAdModal()},
          ]
        });
      }
      items.push({separator: true});
      items.push({label: '로그아웃', icon: 'pi pi-fw pi-power-off', command: () => this.handleLogout()});
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
      // PrimeMenu는 자체 처리
    },
    toggleUserMenu(event) {
      this.$refs.userMenuRef.toggle(event);
      this.showNotificationDropdown = false; // 사용자 메뉴 열 때 알림 닫기
    },
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      // 알림 메뉴 열 때 사용자 메뉴 닫기 (PrimeMenu API 확인 필요, 보통 외부 클릭으로 닫힘)
      // if (this.showNotificationDropdown && this.$refs.userMenuRef?.visible) {
      //   this.$refs.userMenuRef.hide();
      // }
      if (this.showNotificationDropdown && this.isLoggedIn) {
        this.fetchNotifications(); // 열 때마다 최신 정보 로드
      }
    },
    closeDropdown() {
      this.showNotificationDropdown = false;
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
        this.closeDropdown();
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
    formatDate(dateString) { // 상대 시간 포맷 함수
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
      }
    },

    // ***** 수정된 검색 처리 함수 *****
    handleSearch() {
      const query = this.searchQuery.trim(); // 입력된 검색어 가져오기
      if (query) { // 검색어가 있으면
        // '/search' 경로로 이동, 쿼리 파라미터 'q'에 검색어 전달
        this.$router.push({name: 'search', query: {q: query}})
            .catch(err => { // 네비게이션 오류 처리 (옵션)
              if (err.name !== 'NavigationDuplicated' && !err.message.includes('Avoided redundant navigation')) {
                console.error('Search navigation error:', err);
              }
            });
        // 검색 후 입력창 비우기 (선택 사항)
        // this.searchQuery = '';
      }
      // 검색어가 없으면 아무 동작 안함 (또는 알림 표시)
    },
    // ***** /수정된 검색 처리 함수 *****

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
      this.oauthCompletionDataForModal = {email: this.userEmail, oauthName: this.username};
      this.modalInitialTab = 'signup';
      this.showLoginModal = true;
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
    openCreateAdModal() {
      this.isAdModalVisible = true;
    },
    handleAdSaved() {
      this.$toast.add({severity: 'success', summary: '성공', detail: '광고 생성 완료', life: 3000});
    }
  }
};
</script>

<style scoped>
/* --- 전체 스타일 코드 --- */
.navbar {
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  padding: 1rem 0; /* 상하 패딩 조정 */
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.container {
  max-width: 1200px; /* 콘텐츠 최대 너비 */
  margin: 0 auto; /* 중앙 정렬 */
  display: flex;
  align-items: center; /* 수직 중앙 정렬 */
  justify-content: space-between; /* 요소 간 공간 분배 */
  padding: 0 1.5rem; /* 좌우 패딩 */
  box-sizing: border-box; /* 패딩 포함 너비 계산 */
}

.navbar-brand .logo {
  font-size: 1.75rem; /* 로고 크기 */
  font-weight: 700; /* 로고 두께 */
  color: #000; /* 로고 색상 */
  text-decoration: none; /* 밑줄 제거 */
  letter-spacing: -0.5px; /* 글자 간격 */
}

/* 검색창 스타일 */
.search-bar {
  flex-grow: 1; /* 가능한 공간 차지 */
  max-width: 450px; /* 최대 너비 제한 */
  position: relative; /* 버튼 위치 기준 */
  margin: 0 2rem; /* 좌우 마진 */
}

.search-bar input {
  width: 100%; /* 부모 요소 너비 채움 */
  padding: 0.6rem 2.5rem 0.6rem 1rem; /* 패딩 (오른쪽은 아이콘 공간 확보) */
  border: 1px solid #e0e0e0; /* 테두리 */
  border-radius: 4px; /* 모서리 둥글게 */
  font-size: 0.9rem; /* 글자 크기 */
  background-color: #f8f9fa; /* 배경색 */
  box-sizing: border-box; /* 패딩/테두리 포함 너비 계산 */
  transition: border-color 0.2s ease-in-out, box-shadow 0.2s ease-in-out; /* 부드러운 전환 효과 */
}

.search-bar input:focus {
  border-color: #333; /* 포커스 시 테두리 색상 */
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1); /* 포커스 시 그림자 효과 */
  outline: none; /* 기본 아웃라인 제거 */
}

.search-button {
  position: absolute; /* 절대 위치 */
  right: 10px; /* 오른쪽 끝에서 10px */
  top: 50%; /* 수직 중앙 */
  transform: translateY(-50%); /* 정확한 수직 중앙 정렬 */
  border: none; /* 테두리 없음 */
  background: none; /* 배경 없음 */
  color: #666; /* 아이콘 색상 */
  cursor: pointer; /* 마우스 커서 포인터 */
  padding: 5px; /* 클릭 영역 확보 */
}

/* /검색창 스타일 */

/* 메뉴 영역 */
.navbar-menu {
  display: flex;
  align-items: center;
}

/* 알림 메뉴 */
.notification-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative; /* 드롭다운 기준 */
  margin-right: 1.5rem; /* 오른쪽 마진 */
  padding: 5px;
  z-index: 1001; /* 드롭다운이 다른 요소 위에 오도록 */
}

.notification-menu i.fa-bell {
  font-size: 1.3rem; /* 아이콘 크기 */
  color: #333; /* 아이콘 색상 */
}

.notification-badge {
  position: absolute; /* 절대 위치 */
  top: -6px; /* 위치 조정 */
  right: -8px; /* 위치 조정 */
  background-color: #333; /* 배경색 */
  color: white; /* 글자색 */
  border-radius: 50%; /* 원형 */
  padding: 2px 5px; /* 내부 패딩 */
  font-size: 0.7rem; /* 글자 크기 */
  font-weight: 500; /* 글자 두께 */
  line-height: 1; /* 줄 높이 */
  min-width: 16px; /* 최소 너비 */
  text-align: center; /* 중앙 정렬 */
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.3); /* 그림자 */
}

/* /알림 메뉴 */

/* 드롭다운 메뉴 공통 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px); /* 부모 요소 아래 + 간격 */
  right: 0; /* 오른쪽 정렬 */
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 160px; /* 최소 너비 */
  padding: 0.5rem 0; /* 상하 패딩 */
  z-index: 1000; /* 다른 요소 위 */
  display: block;
  list-style: none;
  margin: 0;
}

/* /드롭다운 메뉴 공통 */

/* 알림 드롭다운 상세 */
.notification-dropdown {
  width: 300px; /* 너비 */
  max-height: 400px; /* 최대 높이 */
  overflow-y: auto; /* 내용 많으면 스크롤 */
}

.dropdown-item {
  display: block;
  padding: 0.7rem 1.2rem; /* 패딩 */
  color: #333; /* 글자색 */
  text-decoration: none;
  font-size: 0.9rem;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
  transition: background-color 0.15s ease-in-out;
  white-space: normal; /* 긴 내용 줄바꿈 */
  box-sizing: border-box;
  cursor: pointer;
}

.dropdown-item:hover:not(.mark-all-read-container) {
  background-color: #f5f5f5; /* 호버 시 배경색 */
}

/* 전체 읽음 버튼 컨테이너 */
.mark-all-read-container {
  padding: 0.5rem 1.2rem;
  border-bottom: 1px solid #eee; /* 구분선 */
}

/* 전체 읽음 버튼 */
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

/* 알림 아이템 */
.notification-item {
  display: flex;
  flex-direction: column; /* 세로 배치 (메시지, 시간) */
  padding: 0.8rem 1.2rem;
  border-bottom: 1px solid #f0f0f0; /* 구분선 */
}
.notification-item:last-child {
  border-bottom: none; /* 마지막 아이템 구분선 제거 */
}
.notification-item.unread {
  font-weight: 500; /* 안 읽은 알림 강조 */
}

/* 안 읽은 알림 표시 (점) */
.notification-item.unread::before {
  content: '●';
  color: #007bff; /* 점 색상 */
  font-size: 0.7em;
  margin-right: 8px;
  vertical-align: middle; /* 수직 정렬 */
  float: left; /* 왼쪽으로 띄움 */
  line-height: 1.4; /* 텍스트 줄 높이와 맞춤 */
  padding-top: 2px; /* 미세 조정 */
}

.notification-item:hover {
  background-color: #f5f5f5;
}

.notification-item span {
  margin-bottom: 0.3rem; /* 메시지와 시간 사이 간격 */
  line-height: 1.4; /* 줄 높이 */
  word-break: break-word; /* 긴 단어 줄바꿈 */
}
.notification-item small {
  color: #777; /* 시간 색상 */
  font-size: 0.75rem; /* 시간 글자 크기 */
}

/* 알림 없을 때 메시지 */
.no-notifications {
  padding: 1rem 1.2rem;
  color: #888;
  text-align: center;
  font-size: 0.9rem;
}

/* /알림 드롭다운 상세 */


/* 사용자 메뉴 */
.user-menu {
  display: flex;
  align-items: center;
  position: relative; /* PrimeMenu 기준점 */
  margin-left: 0.5rem; /* 왼쪽 마진 */
  padding: 0;
  z-index: 1001; /* 드롭다운이 다른 요소 위에 오도록 */
}

/* PrimeButton 사용자 정의 (필요시) */
.user-menu-button {
  padding: 0.5rem 0.75rem !important; /* PrimeButton 내부 패딩 강제 조정 */
  /* PrimeVue 기본 스타일 활용 */
}

/* /사용자 메뉴 */


/* PrimeMenu 팝업 스타일 오버라이드 */
:deep(.p-menu) {
  margin-top: 10px !important; /* 버튼과의 간격 */
  min-width: 180px; /* 최소 너비 */
}
:deep(.p-menuitem-link) {
  padding: 0.7rem 1.2rem; /* 아이템 패딩 */
  font-size: 0.9rem; /* 아이템 글자 크기 */
}
:deep(.p-menuitem-icon) {
  margin-right: 0.75rem; /* 아이콘 오른쪽 마진 */
  color: #6c757d; /* 아이콘 색상 */
}

/* 관리자 메뉴 내 아이콘 (예시) */
:deep(.p-menuitem-link .fa-ad) {
  margin-right: 0.5rem;
  color: #6c757d;
}

/* /PrimeMenu 팝업 스타일 */


/* 로그인/회원가입 버튼 영역 */
.auth-buttons {
  display: flex;
  align-items: center;
  gap: 0.75rem; /* 버튼 사이 간격 */
}

/* 로그인/회원가입 버튼 공통 스타일 (PrimeButton 사용) */
.login-button, .signup-button {
  font-weight: 500;
  font-size: 0.9rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  /* PrimeVue 테마 색상 활용 또는 직접 지정 */
}

/* /로그인/회원가입 버튼 */


/* 반응형 스타일 (모바일 등 작은 화면) */
@media (max-width: 768px) {
  .container {
    flex-wrap: wrap; /* 요소 줄바꿈 허용 */
    justify-content: center; /* 중앙 정렬 */
    padding: 0 1rem; /* 좌우 패딩 */
  }
  .navbar-brand {
    width: 100%; /* 로고 전체 너비 */
    text-align: center; /* 로고 중앙 정렬 */
    margin-bottom: 1rem; /* 아래 마진 */
  }
  .search-bar {
    order: 3; /* 순서 변경 (메뉴 아래로) */
    max-width: 100%; /* 전체 너비 */
    margin: 1rem 0 0; /* 위 마진 */
  }
  .navbar-menu {
    order: 2; /* 순서 변경 (검색창 위로) */
    margin-top: 0.5rem; /* 위 마진 */
    width: 100%; /* 전체 너비 */
    justify-content: center; /* 메뉴 중앙 정렬 */
  }
  .user-menu, .notification-menu {
    margin: 0 0.5rem; /* 좌우 마진 조정 */
  }
  .auth-buttons {
    width: 100%; /* 전체 너비 */
    justify-content: center; /* 버튼 중앙 정렬 */
    margin-top: 0.5rem; /* 위 마진 */
  }
  .login-button, .signup-button {
    flex: 1; /* 가능한 공간 동일하게 차지 */
    max-width: 120px; /* 최대 너비 */
    text-align: center; /* 내부 텍스트 중앙 정렬 */
  }
}

/* /반응형 스타일 */

</style>
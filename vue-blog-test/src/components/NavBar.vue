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
          <i class="fas fa-search"></i></button>
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

// PrimeVue 컴포넌트는 main.js에서 전역 등록 ('PrimeButton', 'PrimeMenu' 등)

export default {
  name: 'NavBar', // 컴포넌트 이름
  components: {
    LoginSignupModal, // 로그인/회원가입 모달 등록
    AdvertisementModal // 광고 생성 모달 등록
  },
  data() {
    // 컴포넌트 내부 데이터 상태
    return {
      searchQuery: '', // 검색어
      // showUserDropdown: false, // PrimeMenu 사용으로 제거
      showNotificationDropdown: false, // 알림 드롭다운 표시 여부
      isLoggingOut: false, // 로그아웃 진행 중 상태
      notifications: [], // 알림 목록 배열
      unreadCount: 0, // 안 읽은 알림 개수
      showLoginModal: false, // 로그인/회원가입 모달 표시 여부
      modalInitialTab: 'login', // 모달 초기 탭
      oauthCompletionDataForModal: null, // OAuth 프로필 완성 데이터
      isAdModalVisible: false, // 광고 생성 모달 표시 여부
    };
  },
  computed: {
    // Vuex 상태 매핑 (필요 시)
    ...mapState([
      // 예: 'someOtherState'
    ]),
    // Vuex 게터 매핑
    ...mapGetters([
      'isLoggedIn',       // 로그인 여부
      'userId',           // 사용자 ID
      'username',         // 사용자 이름 (아이디)
      'nickName',         // 사용자 닉네임
      'userEmail',        // 사용자 이메일
      'userProfileImage', // 사용자 프로필 이미지 URL
      'blogUrl',          // 사용자 블로그 URL
      'hasBlog',          // 사용자 블로그 존재 여부
      'isLoading',        // 로딩 상태 (전체)
      'isAdmin'           // 관리자 여부
    ]),
    // 표시할 프로필 이미지 URL 계산
    displayProfileImage() {
      // 사용자 프로필 이미지가 없으면 기본 이미지 경로 반환
      return this.userProfileImage || '/img/default-profile.png'; // 기본 이미지 경로 확인 필요
    },
    // 표시할 닉네임 또는 사용자 이름 계산
    displayNicknameOrUsername() {
      return this.nickName || this.username || '사용자'; // 닉네임 > 이름 > 기본값 순서
    },
    // PrimeMenu 컴포넌트에 전달할 메뉴 아이템 모델 (동적 생성)
    userMenuItems() {
      let items = []; // 빈 배열로 시작

      // 블로그 소유 여부에 따라 '내 블로그' 또는 '블로그 만들기'/'닉네임 설정' 메뉴 추가
      if (this.hasBlog) {
        items.push({
          label: '내 블로그',
          icon: 'pi pi-fw pi-home', // PrimeIcon 사용
          command: () => this.$router.push(`/${this.blogUrl}`).catch(() => {
          }) // 네비게이션 오류 무시
        });
      } else {
        if (this.nickName) { // 닉네임이 있어야 블로그 생성 가능
          items.push({
            label: '블로그 만들기',
            icon: 'pi pi-fw pi-plus',
            command: () => this.$router.push('/blog/create').catch(() => {
            })
          });
        } else { // 닉네임 없으면 설정 유도
          items.push({
            label: '닉네임 설정하기',
            icon: 'pi pi-fw pi-user-plus',
            command: () => this.openProfileCompletionModal()
          });
        }
      }

      // '내 정보 수정' 메뉴 추가
      items.push({
        label: '내 정보 수정',
        icon: 'pi pi-fw pi-user-edit',
        command: () => this.$router.push('/profile/edit').catch(() => {
        })
      });

      // 관리자(isAdmin)일 경우 '관리자 메뉴' 및 하위 '광고 생성' 메뉴 추가
      if (this.isAdmin) {
        items.push({
          separator: true // 구분선
        }, {
          label: '관리자 메뉴',
          icon: 'pi pi-fw pi-cog', // 설정 아이콘
          items: [ // 하위 메뉴 배열
            {
              label: '사용자 관리',
              icon: 'pi pi-fw pi-users',
              command: () => this.$router.push('/admin/users').catch(() => {
              }) // 사용자 관리 페이지 경로 (예시)
            },
            {
              label: '광고 생성',
              icon: 'pi pi-fw pi-plus-circle', // 생성 아이콘
              command: () => this.openCreateAdModal() // 광고 생성 모달 열기 함수 호출
            },
            // 여기에 다른 관리자 메뉴 항목 추가 가능
          ]
        });
      }

      // 구분선 추가
      items.push({separator: true});
      // '로그아웃' 메뉴 추가
      items.push({
        label: '로그아웃',
        icon: 'pi pi-fw pi-power-off',
        command: () => this.handleLogout() // 로그아웃 함수 호출
      });

      // 최종 생성된 메뉴 아이템 배열 반환
      return items;
    }
  },
  watch: {
    // 로그인 상태 변경 감지 (oldValue 파라미터 제거)
    isLoggedIn(newValue) {
      if (newValue) { // 로그인 시
        this.fetchNotifications(); // 알림 가져오기
        this.connectWebSocket(); // 웹소켓 연결
      } else { // 로그아웃 시
        this.notifications = []; // 알림 목록 초기화
        this.unreadCount = 0; // 안 읽은 개수 초기화
        if (websocketService) websocketService.disconnect(); // 웹소켓 연결 해제
      }
    }
  },
  mounted() {
    // 컴포넌트 마운트 시 외부 클릭 감지 리스너 추가
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    // 컴포넌트 파괴 전 리스너 제거 및 웹소켓 연결 해제
    document.removeEventListener('click', this.handleOutsideClick);
    if (websocketService) websocketService.disconnect();
  },
  created() {
    // 컴포넌트 생성 시 OAuth 프로필 완성 필요 여부 확인
    this.checkOAuthCompletionOnLoad();
    // 로그인 상태면 초기 데이터 로드
    if (this.isLoggedIn) {
      this.fetchNotifications();
      this.connectWebSocket();
    }
  },
  methods: {
    // Vuex 액션 매핑
    ...mapActions(['fetchCurrentUser', 'logoutAndClear']), // 실제 스토어 액션 이름 확인

    // 외부 영역 클릭 시 드롭다운 닫기 처리
    handleOutsideClick(event) {
      // PrimeMenu는 자체적으로 외부 클릭 시 닫히므로 사용자 메뉴 관련 로직 불필요
      const notificationMenu = this.$el.querySelector('.notification-menu');
      let clickedInsideNotificationMenu = notificationMenu?.contains(event.target);

      // 알림 메뉴 외부 클릭 시 알림 드롭다운 닫기
      if (!clickedInsideNotificationMenu) {
        this.showNotificationDropdown = false;
      }
    },
    // PrimeMenu 토글 (사용자 메뉴 버튼 클릭 시)
    toggleUserMenu(event) {
      // ref를 통해 PrimeMenu 컴포넌트의 toggle 메소드 호출
      this.$refs.userMenuRef.toggle(event);
      // 사용자 메뉴 열리면 알림 메뉴 닫기
      this.showNotificationDropdown = false;
    },
    // 알림 드롭다운 토글
    toggleNotificationDropdown() {
      this.showNotificationDropdown = !this.showNotificationDropdown;
      // 알림 드롭다운 열리면 사용자 메뉴(PrimeMenu) 닫기 (필요 시)
      if (this.showNotificationDropdown && this.$refs.userMenuRef) {
        // PrimeMenu의 hide() 메소드가 있다면 호출하여 닫을 수 있으나,
        // 보통 다른 영역 클릭 시 자동으로 닫히므로 필수는 아님.
        // this.$refs.userMenuRef.hide();
      }
      // 알림 드롭다운 열 때 최신 알림 정보 가져오기
      if (this.showNotificationDropdown && this.isLoggedIn) {
        this.fetchNotifications();
      }
    },
    // 모든 드롭다운 닫기 (주로 메뉴 아이템 클릭 시 호출)
    closeDropdown() {
      // PrimeMenu는 아이템 클릭 시 자동으로 닫힐 수 있음
      this.showNotificationDropdown = false;
    },
    // 알림 목록 가져오기 메소드 (전체 코드)
    async fetchNotifications() {
      if (!this.isLoggedIn) return; // 로그인 상태 아니면 실행 중단
      try {
        const fetchedNotifications = await authService.getNotifications(); // API 호출
        // isRead 필드 boolean 변환 및 날짜 역순 정렬
        this.notifications = fetchedNotifications.map(n => ({
          ...n, // 기존 알림 데이터 복사
          // API 응답의 read 필드 타입에 따라 boolean으로 변환 (문자열 'true'/'false'도 처리)
          isRead: typeof n.read === 'boolean' ? n.read : String(n.read).toLowerCase() === 'true'
        })).sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate)); // 최신순 정렬

        // 안 읽은 알림 개수 계산
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
      } catch (error) {
        console.error('NavBar: 알림 가져오기 실패:', error);
        this.notifications = []; // 오류 시 초기화
        this.unreadCount = 0; // 오류 시 초기화
      }
    },
    // 웹소켓 연결 메소드 (전체 코드)
    connectWebSocket() {
      // 로그인 상태 및 사용자 ID 확인
      if (!this.isLoggedIn || !this.userId) return;
      // 기존 연결이 있다면 해제
      if (websocketService) websocketService.disconnect();
      // 새 웹소켓 연결 시도 (사용자 ID와 콜백 함수 전달)
      websocketService.connect(this.userId, (notification) => {
        // 새 알림 수신 시 처리 로직
        // 이미 목록에 없는 알림인지 확인
        if (!this.notifications.some(n => n.id === notification.id)) {
          // 새 알림 객체 생성 (isRead 필드 처리 포함)
          const newNotification = {
            ...notification,
            isRead: typeof notification.read === 'boolean' ? notification.read : String(notification.read).toLowerCase() === 'true'
          };
          this.notifications.unshift(newNotification); // 목록 맨 앞에 추가
          this.notifications.sort((a, b) => new Date(b.createdDate) - new Date(a.createdDate)); // 최신순 재정렬
          // 안 읽은 알림이면 카운트 증가
          if (!newNotification.isRead) {
            this.unreadCount += 1;
          }
          // 새 알림 수신 시 Toast 메시지로 사용자에게 알림
          this.$toast.add({severity: 'info', summary: '새 알림', detail: notification.message, life: 5000});
        }
      });
    },
    // 알림 아이템 클릭 시 처리 메소드 (전체 코드)
    async handleNotificationClick(notification) {
      // 유효한 알림 객체인지 확인
      if (!notification || !notification.id) return;
      try {
        // 안 읽은 알림일 경우 서버에 읽음 처리 요청
        if (!notification.isRead) {
          await authService.markNotificationAsRead(notification.id);
          // 로컬 상태 즉시 업데이트 (UI 반응성)
          const targetNotification = this.notifications.find(n => n.id === notification.id);
          if (targetNotification) {
            targetNotification.isRead = true; // 읽음 상태로 변경
          }
          this.unreadCount = Math.max(0, this.unreadCount - 1); // 안 읽은 카운트 감소
        }

        // 알림에 연결된 URL 정보가 있으면 해당 글로 이동
        if (notification.blogUrl && notification.menuId && notification.postId) {
          const targetPath = `/${notification.blogUrl}/${notification.menuId}/${notification.postId}`;
          this.$router.push(targetPath).catch(() => {
          }); // 중복 네비게이션 오류는 무시
        }
        this.closeDropdown(); // 알림 클릭 후 드롭다운 닫기
      } catch (error) {
        console.error('NavBar: 알림 처리 실패:', error);
        // 오류 발생 시 사용자에게 Toast 메시지로 알림
        this.$toast.add({severity: 'error', summary: '오류', detail: '알림 처리 중 오류가 발생했습니다.', life: 3000});
      }
    },
    // '전체 읽음' 버튼 클릭 시 처리 메소드 (전체 코드)
    async handleMarkAllRead() {
      // 안 읽은 알림이 없으면 실행 중단
      if (this.unreadCount === 0) return;
      try {
        // 서버에 모든 알림 읽음 처리 요청
        await authService.markAllNotificationsAsRead();
        // 로컬 상태 즉시 업데이트
        this.notifications.forEach(n => n.isRead = true); // 모든 알림을 읽음 상태로 변경
        this.unreadCount = 0; // 안 읽은 카운트 0으로 설정
        // 성공 메시지 표시
        this.$toast.add({severity: 'success', summary: '알림', detail: '모든 알림을 읽음 처리했습니다.', life: 3000});
        // this.closeDropdown(); // 필요 시 드롭다운 닫기

      } catch (error) {
        console.error('NavBar: 전체 알림 읽음 처리 실패:', error);
        // 오류 메시지 표시
        this.$toast.add({severity: 'error', summary: '오류', detail: '전체 알림 읽음 처리 중 오류가 발생했습니다.', life: 3000});
      }
    },
    // 날짜 포맷팅 함수 (상대 시간 표시) (전체 코드)
    formatDate(dateString) {
      if (!dateString) return ''; // 날짜 문자열 없으면 빈 문자열 반환
      try {
        const date = new Date(dateString); // Date 객체 생성
        const now = new Date(); // 현재 시간
        // 시간 차이 계산 (초, 분, 시간, 일)
        const diffSeconds = Math.round((now - date) / 1000);
        const diffMinutes = Math.round(diffSeconds / 60);
        const diffHours = Math.round(diffMinutes / 60);
        const diffDays = Math.round(diffHours / 24);

        // 시간 차이에 따라 다른 형식으로 반환
        if (diffSeconds < 60) return `${diffSeconds}초 전`;
        if (diffMinutes < 60) return `${diffMinutes}분 전`;
        if (diffHours < 24) return `${diffHours}시간 전`;
        if (diffDays < 7) return `${diffDays}일 전`;
        return date.toLocaleDateString('ko-KR'); // 7일 이상이면 'YYYY. MM. DD.' 형식
      } catch (e) {
        // 날짜 파싱 오류 시 원본 문자열 반환
        return dateString;
      }
    },
    // 로그아웃 처리 함수 (전체 코드)
    async handleLogout() {
      this.isLoggingOut = true; // 로그아웃 진행 중 상태 활성화
      try {
        // Vuex의 logoutAndClear 액션 호출 (비동기)
        await this.logoutAndClear();
        // 로그아웃 성공 시 전역 이벤트 발생 (다른 컴포넌트 알림용, 필요시)
        window.dispatchEvent(new CustomEvent('auth-state-changed'));
        // 현재 경로가 홈('/')이 아니면 홈으로 이동
        if (this.$route.path !== '/') {
          this.$router.push('/').catch(() => {
          }); // 중복 네비게이션 오류 무시
        }
        // 로그아웃 성공 Toast 메시지
        this.$toast.add({severity: 'info', summary: '로그아웃', detail: '로그아웃 되었습니다.', life: 3000});
      } catch (error) {
        // 로그아웃 실패 시 오류 처리
        console.error("NavBar: 로그아웃 오류", error);
        this.$toast.add({severity: 'error', summary: '오류', detail: '로그아웃 처리 중 오류가 발생했습니다.', life: 3000});
      } finally {
        // 로그아웃 처리 완료 후 로딩 상태 해제
        this.isLoggingOut = false;
        // this.closeDropdown(); // PrimeMenu 사용 시 드롭다운 자동 닫힘 가능성 있음
      }
    },
    // 검색 처리 함수 (전체 코드)
    handleSearch() {
      const query = this.searchQuery.trim(); // 입력값 앞뒤 공백 제거
      if (query) { // 검색어가 있을 경우
        // 검색 결과 페이지로 이동 (쿼리 파라미터 포함)
        this.$router.push({path: '/search', query: {q: query}}).catch(() => {
        }); // 실제 검색 페이지 경로 확인
        this.searchQuery = ''; // 검색창 비우기
      }
    },
    // 페이지 로드 시 OAuth 프로필 완성 필요 여부 확인 함수 (전체 코드)
    checkOAuthCompletionOnLoad() {
      // 세션 스토리지에서 관련 데이터 확인
      const completionDataString = sessionStorage.getItem('oauth_profile_completion');
      if (completionDataString) { // 데이터가 있으면
        try {
          // JSON 파싱하여 모달 데이터 설정
          this.oauthCompletionDataForModal = JSON.parse(completionDataString);
          this.modalInitialTab = 'signup'; // 회원가입 탭(프로필 완성 폼 있는 곳)으로 설정
          this.showLoginModal = true; // 로그인/회원가입 모달 열기
          sessionStorage.removeItem('oauth_profile_completion'); // 데이터 사용 후 제거
        } catch (e) {
          // 파싱 오류 시 데이터 제거
          sessionStorage.removeItem('oauth_profile_completion');
          this.oauthCompletionDataForModal = null;
        }
      } else { // 데이터 없으면 null 설정
        this.oauthCompletionDataForModal = null;
      }
    },
    // 로그인 모달 열기 함수 (전체 코드)
    openLoginModal() {
      this.oauthCompletionDataForModal = null; // OAuth 데이터 초기화
      this.modalInitialTab = 'login'; // 로그인 탭으로 시작
      this.showLoginModal = true;
    },
    // 회원가입 모달 열기 함수 (전체 코드)
    openSignupModal() {
      this.oauthCompletionDataForModal = null; // OAuth 데이터 초기화
      this.modalInitialTab = 'signup'; // 회원가입 탭으로 시작
      this.showLoginModal = true;
    },
    // 닉네임 설정 (프로필 완성) 모달 열기 함수 (전체 코드)
    openProfileCompletionModal() {
      // 모달에 전달할 데이터 설정 (현재 사용자 이메일, 이름 등)
      this.oauthCompletionDataForModal = {
        email: this.userEmail, // Vuex 게터 사용
        oauthName: this.username // Vuex 게터 사용 (OAuth 이름으로 username 사용 가정)
      };
      this.modalInitialTab = 'signup'; // 회원가입 탭 (내부에 프로필 완성 폼 가정)
      this.showLoginModal = true;
      // this.closeDropdown(); // PrimeMenu 사용 시 불필요
    },
    // 로그인/회원가입 모달 닫기 함수 (전체 코드)
    closeLoginModal() {
      this.showLoginModal = false;
      this.oauthCompletionDataForModal = null; // 관련 데이터 초기화
    },
    // 로그인/회원가입/프로필 완성 성공 시 후처리 함수 (전체 코드)
    async handleAuthSuccess() {
      this.closeLoginModal(); // 모달 닫기
      try {
        // 최신 사용자 정보 다시 가져오기 (상태 업데이트)
        await this.fetchCurrentUser();
        window.dispatchEvent(new CustomEvent('auth-state-changed')); // 전역 이벤트 발생 (필요시)
      } catch (error) {
        console.error("NavBar: 인증 성공 후 사용자 정보 가져오기 오류:", error);
      }
    },

    // --- 광고 생성 모달 여는 메소드 ---
    openCreateAdModal() {
      this.isAdModalVisible = true; // 광고 모달 표시 상태 true
      // this.closeDropdown(); // PrimeMenu 사용 시 자동으로 닫힐 수 있음
    },
    // --- 광고 생성 모달 저장 완료 시 처리 메소드 ---
    handleAdSaved() {
      // isAdModalVisible = false; // v-model:visible 사용으로 부모에서 직접 변경 불필요
      // 저장 성공 Toast 메시지 표시
      this.$toast.add({severity: 'success', summary: '성공', detail: '광고가 성공적으로 생성되었습니다.', life: 3000});
    }
    // --- ---
  }
};
</script>

<style scoped>
/* --- 전체 스타일 코드는 이전 답변과 동일하게 유지 --- */
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

.dropdown-item:hover:not(.mark-all-read-container) {
  background-color: #f5f5f5;
}

.mark-all-read-container {
  padding: 0.5rem 1.2rem;
  border-bottom: 1px solid #eee;
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
  position: relative;
  margin-left: 0.5rem;
  padding: 0;
  z-index: 1001;
}

.user-menu-button {
  padding: 0.5rem 0.75rem !important; /* PrimeButton 패딩 조정 */
  /* color: #495057; */ /* PrimeButton 기본 스타일 활용 */
}

/* 기존 .user-name, .fa-chevron-down 스타일 불필요 */

/* PrimeMenu 스타일 커스터마이징 */
:deep(.p-menu) {
  margin-top: 10px !important; /* 버튼 아래 간격 */
  min-width: 180px; /* 메뉴 최소 너비 */
}

:deep(.p-menuitem-link) {
  padding: 0.7rem 1.2rem; /* 아이템 패딩 조정 */
  font-size: 0.9rem; /* 폰트 크기 조정 */
}

:deep(.p-menuitem-icon) {
  margin-right: 0.75rem; /* 아이콘과 텍스트 간격 */
  color: #6c757d; /* 아이콘 기본 색상 */
}

/* 관리자 메뉴 아이콘 스타일 */
:deep(.p-menuitem-link .fa-ad) {
  margin-right: 0.5rem;
  color: #6c757d;
}

.auth-buttons {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.login-button, .signup-button {
  font-weight: 500;
  font-size: 0.9rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

/* PrimeButton 클래스로 스타일 제어 */
/* .login-button { ... } */
/* .signup-button { ... } */


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
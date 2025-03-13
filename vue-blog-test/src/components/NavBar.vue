<template>
  <div class="navbar">
    <div class="container">
      <div class="navbar-brand">
        <router-link to="/" class="logo">Blog Platform</router-link>
      </div>

      <div class="search-bar">
        <input
            type="text"
            placeholder="검색어를 입력하세요"
            v-model="searchQuery"
            @keyup.enter="handleSearch"
        >
        <button class="search-button" @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>
      </div>

      <div class="navbar-menu">
        <template v-if="isLoggedIn">
          <div class="user-menu" @click.stop="toggleUserDropdown">
            <span class="user-name">{{ userName }}</span>
            <i class="fas fa-chevron-down"></i>

            <div class="dropdown-menu" v-if="showUserDropdown">
              <template v-if="hasBlog">
                <router-link to="/my-blog" class="dropdown-item">내 블로그</router-link>
                <router-link to="/profile" class="dropdown-item">프로필 설정</router-link>
              </template>
              <template v-else>
                <router-link to="/blog/create" class="dropdown-item" @click.stop="logNavigation">블로그 생성하기</router-link>
              </template>
              <div class="dropdown-divider"></div>
              <button @click.stop="handleLogout" class="dropdown-item" :disabled="isLoggingOut">
                {{ isLoggingOut ? '로그아웃 중...' : '로그아웃' }}
              </button>
            </div>
          </div>

          <router-link to="/write" class="write-button">
            <i class="fas fa-pen"></i> 글쓰기
          </router-link>
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

export default {
  name: 'NavBar',
  data() {
    return {
      searchQuery: '',
      showUserDropdown: false,
      isLoggedIn: false,
      userName: '',
      userProfileImage: '',
      isLoggingOut: false,
      isToggling: false,
      hasBlog: false,
      userId: null
    };
  },
  created() {
    this.checkLoginStatus(); // 한 번만 호출
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeDropdown);
  },
  methods: {
    async checkLoginStatus() {
      const user = authService.getStoredUser();
      console.log('Stored User:', user);
      if (user) {
        this.isLoggedIn = true;
        this.userName = user.username;
        this.userId = user.id;
        this.userProfileImage = user.profileImage || '';
        await this.checkBlogStatus(); // 비동기 호출 최소화
      } else {
        console.log('No valid user, staying logged out');
        this.isLoggedIn = false;
        this.userName = '';
        this.userId = null;
        this.userProfileImage = '';
        this.hasBlog = false;
        // 서버 호출 필요 시 여기서 getCurrentUser 호출
        try {
          const serverUser = await authService.getCurrentUser();
          authService.setStoredUser({ id: serverUser.userId, username: serverUser.username });
          this.checkLoginStatus(); // 재귀 호출 대신 상태 갱신
        } catch (error) {
          console.error('Failed to fetch current user:', error);
        }
      }
    },
    async checkBlogStatus() {
      if (!this.userId) {
        console.log('No userId, skipping blog status check');
        this.hasBlog = false;
        return;
      }
      try {
        console.log('Checking blog status for userId:', this.userId);
        const response = await authService.getBlogByUserId(this.userId);
        this.hasBlog = response !== null && response.id !== undefined;
        console.log('Blog check response:', response);
        console.log('Has blog:', this.hasBlog);
      } catch (error) {
        console.error('블로그 상태 확인 실패:', error);
        this.hasBlog = false;
      }
    },
    toggleUserDropdown() {
      if (this.isToggling) return;
      this.isToggling = true;
      console.log('Toggle triggered, current state:', this.showUserDropdown);
      this.showUserDropdown = !this.showUserDropdown;
      console.log('Dropdown toggled:', this.showUserDropdown);
      this.$nextTick(() => {
        console.log('DOM updated, showUserDropdown:', this.showUserDropdown);
        const dropdown = document.querySelector('.dropdown-menu');
        console.log('Dropdown element exists:', !!dropdown);
        this.isToggling = false;
      });
    },
    closeDropdown(event) {
      if (!this.showUserDropdown || this.isToggling) return;
      if (!event.target.closest('.user-menu')) {
        console.log('Closing dropdown, target:', event.target);
        this.showUserDropdown = false;
        console.log('Dropdown closed');
      }
    },
    logNavigation() {
      console.log('Navigating to /blog/create');
      this.showUserDropdown = false;
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
        this.$router.push('/login');
        alert('로그아웃되었습니다.');
      } catch (error) {
        console.error('로그아웃 중 오류 발생:', error);
        alert('로그아웃에 실패했습니다. 다시 시도해주세요.');
      } finally {
        this.isLoggingOut = false;
      }
    },
    handleSearch() {
      if (this.searchQuery.trim()) {
        this.$router.push({
          path: '/search',
          query: { q: this.searchQuery.trim() }
        });
      }
    }
  }
};
</script>

<style scoped>
/* 기존 스타일 유지 */
.navbar {
  background-color: white;
  border-bottom: 1px solid #e5e5e5;
  padding: 12px 0;
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
  padding: 0 20px;
}

.navbar-brand .logo {
  font-size: 22px;
  font-weight: bold;
  color: #03C75A;
  text-decoration: none;
}

.search-bar {
  flex: 1;
  max-width: 460px;
  position: relative;
  margin: 0 20px;
}

.search-bar input {
  width: 100%;
  padding: 10px 40px 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
}

.search-button {
  position: absolute;
  right: 12px;
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

.user-menu {
  display: flex;
  align-items: center;
  cursor: pointer;
  position: relative;
  margin-right: 15px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  margin-right: 5px;
}

.dropdown-menu {
  position: absolute;
  top: 40px;
  right: 0;
  background-color: white;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 160px;
  min-height: 100px;
  z-index: 2000;
  padding: 5px 0;
  display: block;
}

.dropdown-item {
  display: block;
  padding: 10px 15px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  background: none;
  border: none;
  width: 100%;
  text-align: left;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.dropdown-divider {
  height: 1px;
  background-color: #e5e5e5;
  margin: 5px 0;
}

.write-button {
  background-color: #03C75A;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  display: flex;
  align-items: center;
}

.write-button i {
  margin-right: 5px;
}

.login-button {
  color: #03C75A;
  font-weight: 500;
  text-decoration: none;
  font-size: 14px;
  padding: 8px 16px;
  border: 1px solid #03C75A;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .container {
    flex-wrap: wrap;
  }

  .navbar-brand {
    margin-bottom: 10px;
  }

  .search-bar {
    order: 3;
    max-width: 100%;
    margin: 10px 0 0;
  }
}
</style>
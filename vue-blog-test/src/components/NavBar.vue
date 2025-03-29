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
          <div
              class="user-menu"
              @click.stop="toggleUserDropdown"
          >
            <span class="user-name">{{ userName }}</span>
            <i class="fas fa-chevron-down"></i>

            <div
                v-if="showUserDropdown"
                class="dropdown-menu"
                @click.stop
            >
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
      hasBlog: false,
      userId: null,
      blogUrl: '',
    };
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
  },
  created() {
    this.checkLoginStatus();
  },
  methods: {
    handleOutsideClick(event) {
      const userMenu = this.$el.querySelector('.user-menu');
      if (userMenu && !userMenu.contains(event.target)) {
        console.log('Clicked outside, closing dropdown');
        this.showUserDropdown = false;
      }
    },
    toggleUserDropdown() {
      console.log('Toggling user dropdown, current state:', this.showUserDropdown);
      this.showUserDropdown = !this.showUserDropdown;
    },
    closeDropdown() {
      this.showUserDropdown = false;
    },
    async checkLoginStatus() {
      const user = authService.getStoredUser();
      if (user) {
        this.isLoggedIn = true;
        this.userName = user.username;
        this.userId = user.id;
        this.userProfileImage = user.profileImage || '';
        await this.checkBlogStatus();
      } else {
        this.isLoggedIn = false;
        this.userName = '';
        this.userId = null;
        this.userProfileImage = '';
        this.hasBlog = false;
        this.blogUrl = '';
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
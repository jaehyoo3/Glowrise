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
          <div class="user-menu" @click="toggleUserDropdown">
            <img :src="userProfileImage || '/img/default-avatar.png'" alt="프로필" class="profile-image">
            <span class="user-name">{{ userName }}</span>
            <i class="fas fa-chevron-down"></i>

            <div class="dropdown-menu" v-show="showUserDropdown">
              <router-link to="/my-blog" class="dropdown-item">내 블로그</router-link>
              <router-link to="/profile" class="dropdown-item">프로필 설정</router-link>
              <div class="dropdown-divider"></div>
              <button @click="handleLogout" class="dropdown-item">로그아웃</button>
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
      userProfileImage: ''
    }
  },
  created() {
    this.checkLoginStatus();
    // 클릭 이벤트를 감지하여 드롭다운 메뉴 닫기
    document.addEventListener('click', this.closeDropdown);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.closeDropdown);
  },
  methods: {
    checkLoginStatus() {
      const user = authService.getStoredUser();
      if (user) {
        this.isLoggedIn = true;
        this.userName = user.nickName || user.username;
        this.userProfileImage = user.profileImage;
      } else {
        this.isLoggedIn = false;
        this.userName = '';
        this.userProfileImage = '';
      }
    },
    toggleUserDropdown(event) {
      event.stopPropagation();
      this.showUserDropdown = !this.showUserDropdown;
    },
    closeDropdown(event) {
      if (!event.target.closest('.user-menu')) {
        this.showUserDropdown = false;
      }
    },
    async handleLogout() {
      try {
        await authService.logout();
        this.isLoggedIn = false;
        this.$router.push('/login');
      } catch (error) {
        console.error('로그아웃 중 오류 발생:', error);
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
}
</script>

<style scoped>
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

.profile-image {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 8px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  margin-right: 5px;
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
  margin-top: 10px;
  z-index: 10;
}

.dropdown-item {
  display: block;
  padding: 10px 15px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
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
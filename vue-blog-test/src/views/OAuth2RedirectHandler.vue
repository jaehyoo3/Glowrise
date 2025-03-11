<template>
  <div class="oauth-redirect">
    <div class="loading-spinner">
      <div class="spinner"></div>
      <p>로그인 처리 중입니다...</p>
    </div>
  </div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'OAuth2RedirectHandler',
  async mounted() {
    try {
      // 사용자 정보 가져오기
      const user = await authService.getCurrentUser();
      authService.setStoredUser(user);
      this.$router.push('/');
    } catch (error) {
      console.error('OAuth 로그인 처리 중 오류 발생:', error);
      this.$router.push('/login');
    }
  }
};
</script>

<style scoped>
.oauth-redirect {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f6f7;
}

.loading-spinner {
  text-align: center;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border-left-color: #03C75A;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
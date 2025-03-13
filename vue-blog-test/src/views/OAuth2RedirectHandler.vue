<template>
  <div>Redirecting...</div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'OAuth2RedirectHandler',
  async mounted() {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const accessToken = urlParams.get('accessToken');
      const refreshToken = urlParams.get('refreshToken');

      if (accessToken && refreshToken) {
        console.log('Received tokens from OAuth2 redirect:', { accessToken, refreshToken });
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);

        const response = await authService.getCurrentUser();
        const { username, id } = response;
        console.log('User info after OAuth2:', response);
        authService.setStoredUser({ id, username });
      } else {
        console.error('No tokens found in OAuth2 redirect URL');
        throw new Error('Tokens not provided in redirect');
      }

      this.$router.push('/');
    } catch (error) {
      console.error('OAuth 로그인 처리 중 오류 발생:', error);
      this.$router.push('/login');
    }
  },
};
</script>
<template>
  <div class="container mt-5">
    <h2>Login</h2>
    <form @submit.prevent="login">
      <div class="mb-3"><input v-model="form.email" class="form-control" placeholder="Email" required /></div>
      <div class="mb-3"><input v-model="form.password" class="form-control" placeholder="Password" type="password" required /></div>
      <button type="submit" class="btn btn-primary">Login</button>
      <router-link to="/signup" class="btn btn-secondary ms-2">Sign Up</router-link>
    </form>
    <div class="mt-3">
      <button class="btn btn-danger" @click="oauth2Login('google')">Google Login</button>
      <button class="btn btn-warning ms-2" @click="oauth2Login('naver')">Naver Login</button>
    </div>
  </div>
</template>

<script>
import { api } from '../axios';

export default {
  data: () => ({
    form: { email: '', password: '' },
  }),
  methods: {
    async login() {
      try {
        await api.post('/login', new URLSearchParams({
          email: this.form.email,
          password: this.form.password,
        }), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          withCredentials: true, // 쿠키를 포함하기 위해 설정
        });
        // 로그인 성공 시 쿠키는 백엔드에서 설정됨. 인증 상태 확인 후 리다이렉트
        await this.checkAuth();
        alert('로그인 성공!');
        this.$router.push('/');
      } catch (error) {
        alert('로그인 실패: ' + (error.response?.data || error.message));
      }
    },
    oauth2Login(provider) {
      window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    },
    async checkAuth() {
      try {
        // 인증 상태 확인용 API 호출 (예: 사용자 프로필 조회)
        await api.get(`/users/profile/${this.form.email.split('@')[0]}`); // username 가정
      } catch (error) {
        console.error('인증 확인 실패:', error);
        throw error;
      }
    },
  },
};
</script>
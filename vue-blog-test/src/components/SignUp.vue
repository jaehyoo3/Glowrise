<template>
  <div class="container mt-5">
    <h2>Sign Up</h2>
    <form @submit.prevent="signUp">
      <div class="mb-3"><input v-model="form.username" class="form-control" placeholder="Username" required /></div>
      <div class="mb-3"><input v-model="form.email" class="form-control" placeholder="Email" type="email" required /></div>
      <div class="mb-3"><input v-model="form.password" class="form-control" placeholder="Password" type="password" required /></div>
      <div class="mb-3"><input v-model="form.nickName" class="form-control" placeholder="Nickname" required /></div>
      <button type="submit" class="btn btn-primary">Sign Up</button>
      <router-link to="/login" class="btn btn-secondary ms-2">Back to Login</router-link>
    </form>
  </div>
</template>

<script>
import { api } from '../axios';

export default {
  data: () => ({
    form: { username: '', email: '', password: '', nickName: '' },
  }),
  methods: {
    async signUp() {
      try {
        await api.post('/users/signup', this.form);
        alert('회원가입 성공! 로그인 페이지로 이동합니다.');
        this.$router.push('/login');
      } catch (error) {
        alert('회원가입 실패: ' + (error.response?.data?.message || error.message));
      }
    },
  },
};
</script>
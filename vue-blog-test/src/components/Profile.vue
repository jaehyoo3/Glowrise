<template>
  <div class="container mt-5">
    <h2>Profile</h2>
    <div v-if="user">
      <p>Username: {{ user.username }}</p>
      <p>Email: {{ user.email }}</p>
      <p>Nickname: {{ user.nickName }}</p>
      <form @submit.prevent="updateProfile">
        <div class="mb-3"><input v-model="form.email" class="form-control" placeholder="New Email" /></div>
        <div class="mb-3"><input v-model="form.nickName" class="form-control" placeholder="New Nickname" /></div>
        <button type="submit" class="btn btn-primary">Update</button>
      </form>
    </div>
  </div>
</template>

<script>
import { api } from '../axios';

export default {
  data: () => ({
    user: null,
    form: { email: '', nickName: '' },
  }),
  mounted() {
    this.fetchProfile();
  },
  methods: {
    async fetchProfile() {
      try {
        const response = await api.get(`/users/profile/${this.$route.params.username}`);
        this.user = response.data;
        this.form.email = this.user.email;
        this.form.nickName = this.user.nickName;
      } catch (error) {
        alert('프로필 조회 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async updateProfile() {
      try {
        const response = await api.put(`/users/profile/${this.$route.params.username}`, this.form);
        this.user = response.data;
        alert('프로필 수정 성공!');
      } catch (error) {
        alert('프로필 수정 실패: ' + (error.response?.data?.message || error.message));
      }
    },
  },
};
</script>
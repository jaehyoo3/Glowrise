<template>
  <div class="blog-create container mt-4">
    <h1>블로그 생성</h1>
    <form @submit.prevent="handleCreateBlog">
      <div class="form-group">
        <label for="title">블로그 제목</label>
        <input
            type="text"
            id="title"
            v-model="form.title"
            class="form-control"
            placeholder="블로그 제목을 입력하세요"
            required
        >
      </div>
      <div class="form-group">
        <label for="description">설명</label>
        <textarea
            id="description"
            v-model="form.description"
            class="form-control"
            placeholder="블로그에 대한 설명을 입력하세요"
        ></textarea>
      </div>
      <div class="form-group">
        <label for="url">URL</label>
        <input
            type="text"
            id="url"
            v-model="form.url"
            class="form-control"
            placeholder="고유한 URL을 입력하세요 (영문, 숫자, -만 가능)"
            @blur="checkUrl"
            required
        >
        <small v-if="urlError" class="text-danger">{{ urlError }}</small>
        <small v-else-if="urlAvailable" class="text-success">사용 가능한 URL입니다.</small>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="!urlAvailable">생성하기</button>
    </form>
  </div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'BlogCreateView',
  data() {
    return {
      form: { title: '', description: '', url: '' },
      urlAvailable: false,
      urlError: ''
    };
  },
  methods: {
    async checkUrl() {
      this.urlError = '';
      this.urlAvailable = false;
      const urlPattern = /^[a-zA-Z0-9-]+$/;
      if (!urlPattern.test(this.form.url)) {
        this.urlError = 'URL은 영문, 숫자, 하이픈(-)만 사용할 수 있습니다.';
        return;
      }
      try {
        const available = await authService.checkUrlAvailability(this.form.url);
        this.urlAvailable = available;
        if (!available) {
          this.urlError = '이미 사용 중인 URL입니다.';
        }
      } catch (error) {
        this.urlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },
    async handleCreateBlog() {
      try {
        const user = await authService.getCurrentUser();
        await authService.createBlog(this.form, user.id);
        alert('블로그가 생성되었습니다!');
        this.$router.push('/'); // 홈으로 이동, NavBar에서 checkBlogStatus 호출됨
      } catch (error) {
        alert('블로그 생성 실패: ' + (error.response?.data?.message || error.message));
      }
    }
  }
};
</script>

<style scoped>
.form-group {
  margin-bottom: 20px;
}
</style>
<template>
  <div class="blog-edit container mt-4">
    <h1>블로그 수정</h1>
    <form @submit.prevent="handleUpdateBlog">
      <div class="form-group">
        <label for="title">블로그 제목</label>
        <input
            type="text"
            id="title"
            v-model="form.title"
            class="form-control"
            required
        >
      </div>
      <div class="form-group">
        <label for="description">설명</label>
        <textarea
            id="description"
            v-model="form.description"
            class="form-control"
        ></textarea>
      </div>
      <div class="form-group">
        <label for="url">URL</label>
        <input
            type="text"
            id="url"
            v-model="form.url"
            class="form-control"
            @blur="checkUrl"
            required
        >
        <small v-if="urlError" class="text-danger">{{ urlError }}</small>
        <small v-else-if="urlAvailable" class="text-success">사용 가능한 URL입니다.</small>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="!urlAvailable">수정하기</button>
      <button type="button" class="btn btn-danger ms-2" @click="handleDeleteBlog">삭제하기</button>
    </form>
  </div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'BlogEditView',
  data() {
    return {
      form: { title: '', description: '', url: '' },
      blogId: null,
      urlAvailable: true,
      urlError: ''
    };
  },
  async created() {
    await this.loadBlog();
  },
  methods: {
    async loadBlog() {
      try {
        const user = await authService.getCurrentUser();
        const blog = await authService.getBlogByUserId(user.id);
        if (blog) {
          this.form = { title: blog.title, description: blog.description, url: blog.url };
          this.blogId = blog.id;
        } else {
          this.$router.push('/');
        }
      } catch (error) {
        console.error('블로그 로드 실패:', error);
        this.$router.push('/');
      }
    },
    async checkUrl() {
      if (this.form.url === this.originalUrl) {
        this.urlAvailable = true;
        this.urlError = '';
        return;
      }
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
    async handleUpdateBlog() {
      try {
        const user = await authService.getCurrentUser();
        await authService.updateBlog(this.blogId, this.form, user.id);
        alert('블로그가 수정되었습니다!');
        this.$router.push('/');
      } catch (error) {
        alert('블로그 수정 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async handleDeleteBlog() {
      if (confirm('정말 블로그를 삭제하시겠습니까?')) {
        try {
          const user = await authService.getCurrentUser();
          await authService.deleteBlog(this.blogId, user.id);
          alert('블로그가 삭제되었습니다!');
          this.$router.push('/');
        } catch (error) {
          alert('블로그 삭제 실패: ' + (error.response?.data?.message || error.message));
        }
      }
    }
  },
  computed: {
    originalUrl() {
      return this.blogId ? this.form.url : '';
    }
  }
};
</script>

<style scoped>
.form-group {
  margin-bottom: 20px;
}
</style>
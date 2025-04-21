<template>
  <div class="blog-create">
    <div class="container">
      <div class="blog-create-content">
        <div class="blog-create-header">
          <h1>블로그 만들기</h1>
          <p>당신만의 이야기를 시작하세요</p>
        </div>

        <form @submit.prevent="handleCreateBlog" class="blog-create-form">
          <div class="form-group">
            <label for="title">블로그 제목</label>
            <input
                type="text"
                id="title"
                v-model="form.title"
                placeholder="블로그 제목을 입력하세요"
                required
                class="form-input"
            >
          </div>

          <div class="form-group">
            <label for="description">설명</label>
            <textarea
                id="description"
                v-model="form.description"
                placeholder="블로그에 대한 설명을 입력하세요"
                class="form-input"
                rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="url">URL</label>
            <div class="url-input-wrapper">
              <span class="url-prefix">blog.com/</span>
              <input
                  id="url"
                  v-model="form.url"
                  :class="{ 'input-error': urlError, 'input-success': urlAvailable && form.url }"
                  class="form-input url-input"
                  placeholder="your-blog-name"
                  required
                  type="text"
                  @blur="checkUrl"
              >
            </div>
            <div v-if="urlError" class="input-feedback error">
              <span class="feedback-icon">!</span> {{ urlError }}
            </div>
            <div v-else-if="urlAvailable && form.url" class="input-feedback success">
              <span class="feedback-icon">✓</span> 사용 가능한 URL입니다.
            </div>
          </div>

          <div class="form-actions">
            <button
                type="submit"
                class="submit-button"
                :class="{ 'button-loading': isSubmitting }"
                :disabled="!urlAvailable || !form.title"
            >
              <span v-if="isSubmitting">처리 중...</span>
              <span v-else>블로그 생성하기</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex';
import authService from '@/services/authService';

export default {
  name: 'BlogCreateView',
  data() {
    return {
      form: {
        title: '',
        description: '',
        url: '',
        userId: null
      },
      urlAvailable: false,
      urlError: '',
      isSubmitting: false,
    };
  },
  computed: {
    ...mapGetters(['userId'])
  },
  methods: {
    ...mapActions(['fetchUserBlog']),

    async checkUrl() {
      this.urlError = '';
      this.urlAvailable = false;
      const urlPattern = /^[a-zA-Z0-9-]+$/;
      if (!this.form.url) {
        this.urlError = 'URL을 입력해주세요.';
        return;
      }
      if (!urlPattern.test(this.form.url)) {
        this.urlError = 'URL은 영문, 숫자, 하이픈(-)만 사용할 수 있습니다.';
        return;
      }
      try {
        const available = await authService.checkBlogUrlAvailability(this.form.url);
        this.urlAvailable = available;
        if (!available) {
          this.urlError = '이미 사용 중인 URL입니다.';
        }
      } catch (error) {
        this.urlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },
    async handleCreateBlog() {
      if (!this.urlAvailable || this.urlError) {
        alert('URL을 확인해주세요.');
        return;
      }
      if (!this.form.title) {
        alert('블로그 제목을 입력해주세요.');
        return;
      }

      if (!this.userId) {
        alert('사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.');
        this.$router.push('/login');
        return;
      }
      this.form.userId = this.userId;

      this.isSubmitting = true;
      try {
        await authService.createBlog(this.form);
        await this.fetchUserBlog();
        this.$router.push('/');
        alert('블로그가 성공적으로 생성되었습니다!');
      } catch (error) {
        alert('블로그 생성 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmitting = false;
      }
    }
  }
};
</script>

<style scoped>
.blog-create {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
  padding: 1rem;
}

.container {
  width: 100%;
  max-width: 550px;
}

.blog-create-content {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
  padding: 2.5rem;
}

.blog-create-header {
  text-align: center;
  margin-bottom: 2rem;
}

.blog-create-header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 0.5rem;
}

.blog-create-header p {
  color: #666;
  font-size: 1.1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
  font-size: 0.95rem;
}

.form-input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  transition: all 0.2s ease;
  background-color: white;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: #aaa;
}

.form-input.input-error {
  border-color: #ef4444;
  background-color: #fff8f8;
}

.form-input.input-success {
  border-color: #10b981;
  background-color: #f0fdf4;
}

.url-input-wrapper {
  display: flex;
  align-items: center;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.url-prefix {
  padding: 0.8rem 0.5rem 0.8rem 1rem;
  background-color: #f5f5f5;
  color: #666;
  font-size: 0.95rem;
  white-space: nowrap;
  border-right: 1px solid #e0e0e0;
}

.url-input {
  border: none;
  border-radius: 0;
  flex: 1;
}

.url-input:focus {
  box-shadow: none;
}

.url-input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-feedback {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
}

.feedback-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  margin-right: 0.5rem;
  font-size: 12px;
}

.input-feedback.error {
  color: #ef4444;
}

.input-feedback.error .feedback-icon {
  background-color: #ef4444;
  color: white;
}

.input-feedback.success {
  color: #10b981;
}

.input-feedback.success .feedback-icon {
  background-color: #10b981;
  color: white;
}

.form-actions {
  margin-top: 2rem;
}

.submit-button {
  width: 100%;
  background-color: #000;
  color: white;
  border: none;
  padding: 0.9rem 1rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-button:hover:not(:disabled) {
  background-color: #1a1a1a;
  transform: translateY(-1px);
}

.submit-button:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.button-loading {
  opacity: 0.8;
  cursor: wait;
}

@media (max-width: 640px) {
  .blog-create-content {
    padding: 1.5rem;
  }

  .blog-create-header h1 {
    font-size: 1.75rem;
  }

  .blog-create-header p {
    font-size: 1rem;
  }
}
</style>
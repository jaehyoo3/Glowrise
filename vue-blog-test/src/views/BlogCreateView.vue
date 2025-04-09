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
                rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="url">URL</label>
            <input
                type="text"
                id="url"
                v-model="form.url"
                placeholder="고유한 URL을 입력하세요 (영문, 숫자, -만 가능)"
                @blur="checkUrl"
                required
                class="form-input"
                :class="{ 'input-error': urlError, 'input-success': urlAvailable }"
            >
            <div v-if="urlError" class="input-feedback error">
              {{ urlError }}
            </div>
            <div v-else-if="urlAvailable" class="input-feedback success">
              사용 가능한 URL입니다.
            </div>
          </div>

          <div class="form-actions">
            <button
                type="submit"
                class="submit-button"
                :disabled="!urlAvailable"
            >
              블로그 생성
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import {mapActions, mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import

export default {
  name: 'BlogCreateView',
  data() {
    return {
      form: {
        title: '',
        description: '',
        url: '',
        userId: null // userId는 이제 스토어에서 가져옴
      },
      urlAvailable: false,
      urlError: '',
      isSubmitting: false, // 제출 중 상태 추가
    };
  },
  computed: {
    // 스토어 getter 매핑
    ...mapGetters(['userId']) // 현재 로그인된 사용자의 ID 가져오기
  },
  methods: {
    // 스토어 액션 매핑
    ...mapActions(['fetchUserBlog']), // 블로그 생성 후 상태 갱신용

    async checkUrl() {
      // URL 유효성 검사 및 중복 확인 로직 (기존과 동일)
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
        // authService 직접 호출 유지
        const available = await authService.checkBlogUrlAvailability(this.form.url);
        this.urlAvailable = available;
        if (!available) {
          this.urlError = '이미 사용 중인 URL입니다.';
        } else {
          this.urlError = '사용 가능한 URL입니다.'; // 성공 메시지
        }
      } catch (error) {
        this.urlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },
    async handleCreateBlog() {
      // URL 유효성 및 사용 가능 여부 최종 확인
      if (!this.urlAvailable || this.urlError === '이미 사용 중인 URL입니다.' || this.urlError === 'URL 확인 중 오류가 발생했습니다.' || this.urlError.includes('URL은 영문')) {
        alert('URL을 확인해주세요.');
        return;
      }
      if (!this.form.title) {
        alert('블로그 제목을 입력해주세요.');
        return;
      }

      // 스토어에서 userId 가져와서 form에 설정
      if (!this.userId) {
        alert('사용자 정보를 가져올 수 없습니다. 다시 로그인해주세요.');
        // 로그인 페이지로 리디렉션 등 처리
        this.$router.push('/login'); // 예시
        return;
      }
      this.form.userId = this.userId;

      this.isSubmitting = true;
      try {
        // authService 직접 호출하여 블로그 생성
        await authService.createBlog(this.form);
        alert('블로그가 생성되었습니다!');

        // --- Vuex: 생성 성공 후, 스토어의 블로그 정보 갱신 ---
        await this.fetchUserBlog(); // 사용자의 블로그 정보를 다시 불러옴
        // --------------------------------------------------

        // 사용자의 새 블로그 URL로 이동 (스토어 갱신 후 blogUrl getter 사용 가능)
        // 또는 홈으로 이동
        // const blogUrl = this.$store.getters.blogUrl; // 스토어 getter 접근 (만약 즉시 갱신된다면)
        // if (blogUrl) {
        //     this.$router.push(`/${blogUrl}`);
        // } else {
        this.$router.push('/'); // 우선 홈으로 이동
        // }

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
  background-color: #f8f9fa;
  min-height: 100vh;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem;
}

.blog-create-content {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  padding: 2rem;
}

.blog-create-header {
  text-align: center;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 1rem;
}

.blog-create-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  margin-bottom: 0.5rem;
}

.blog-create-header p {
  color: #666;
  font-size: 1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #000;
}

.form-input.input-error {
  border-color: #dc3545;
}

.form-input.input-success {
  border-color: #28a745;
}

.input-feedback {
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

.input-feedback.error {
  color: #dc3545;
}

.input-feedback.success {
  color: #28a745;
}

.form-actions {
  margin-top: 2rem;
  text-align: center;
}

.submit-button {
  background-color: #000;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .blog-create-header h1 {
    font-size: 2rem;
  }
}
</style>
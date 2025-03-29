<template>
  <div class="user-profile-edit container mt-4">
    <NavBar/>
    <h1>내 정보 수정</h1>
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="user">
      <form @submit.prevent="submitForm" class="profile-form">
        <!-- 사용자 이름 (읽기 전용) -->
        <div class="form-group">
          <label for="username">사용자 이름</label>
          <input
              type="text"
              id="username"
              v-model="formData.username"
              class="form-control"
              disabled
          />
        </div>

        <!-- 이메일 (읽기 전용) -->
        <div class="form-group">
          <label for="email">이메일</label>
          <input
              type="email"
              id="email"
              v-model="formData.email"
              class="form-control"
              disabled
          />
        </div>

        <!-- 닉네임 -->
        <div class="form-group">
          <label for="nickName">닉네임</label>
          <input
              type="text"
              id="nickName"
              v-model="formData.nickName"
              class="form-control"
              placeholder="닉네임을 입력하세요"
              required
          />
        </div>

        <!-- 비밀번호 (로컬 사용자만 수정 가능) -->
        <div v-if="isLocalUser" class="form-group">
          <label for="password">새 비밀번호</label>
          <input
              type="password"
              id="password"
              v-model="formData.password"
              class="form-control"
              placeholder="새 비밀번호를 입력하세요 (선택)"
          />
          <small class="form-text text-muted">
            비밀번호를 변경하지 않으려면 비워두세요.
          </small>
        </div>

        <!-- 제출 버튼 -->
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? '저장 중...' : '저장' }}
          </button>
          <router-link to="/" class="btn btn-secondary ml-2">취소</router-link>
        </div>
      </form>
    </div>
    <div v-else>
      <h1>사용자 정보를 불러올 수 없습니다.</h1>
      <router-link to="/" class="btn btn-primary">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'UserProfileEdit',
  components: {NavBar},
  data() {
    return {
      user: null,
      formData: {
        username: '',
        email: '',
        nickName: '',
        password: '',
      },
      isLocalUser: false,
      isLoading: true,
      isSubmitting: false,
    };
  },
  async created() {
    await this.loadUserData();
  },
  methods: {
    async loadUserData() {
      try {
        this.isLoading = true;
        const user = await authService.getCurrentUser();
        this.user = user;
        this.formData.username = user.username;
        this.formData.email = user.email;
        this.formData.nickName = user.nickName || user.username;
        this.isLocalUser = user.site === 'LOCAL'; // 백엔드에서 site가 "LOCAL"인지 확인
      } catch (error) {
        console.error('사용자 정보 로드 실패:', error);
        this.user = null;
        alert('사용자 정보를 불러오는 데 실패했습니다.');
      } finally {
        this.isLoading = false;
      }
    },
    async submitForm() {
      if (this.isSubmitting) return;
      this.isSubmitting = true;

      try {
        const updatedData = {
          nickName: this.formData.nickName,
          password: this.isLocalUser ? this.formData.password : undefined, // 로컬 사용자만 비밀번호 전송
        };

        const updatedUser = await authService.updateUserProfile(this.user.username, updatedData);
        // 로컬 스토리지 업데이트
        authService.setStoredUser({
          id: updatedUser.id,
          username: updatedUser.username,
        });
        alert('정보가 성공적으로 수정되었습니다.');
        this.$router.push('/'); // 홈으로 이동
      } catch (error) {
        console.error('정보 수정 실패:', error);
        alert('정보 수정에 실패했습니다: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmitting = false;
      }
    },
  },
};
</script>

<style scoped>
.user-profile-edit {
  max-width: 600px;
  margin: 0 auto;
}

h1 {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 2rem;
  text-align: center;
}

.profile-form {
  background-color: #fff;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #333;
}

.form-control {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: border-color 0.2s;
}

.form-control:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 5px rgba(0, 123, 255, 0.3);
}

.form-control:disabled {
  background-color: #f8f9fa;
  color: #666;
}

.form-text {
  font-size: 0.8rem;
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 2rem;
}

.btn {
  padding: 0.5rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  border-radius: 4px;
  text-decoration: none;
  transition: background-color 0.2s;
}

.btn-primary {
  background-color: #007bff;
  color: white;
  border: none;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-primary:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
  border: none;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.ml-2 {
  margin-left: 0.5rem;
}
</style>
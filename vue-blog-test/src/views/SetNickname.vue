<template>
  <div class="set-nickname-container">
    <div class="set-nickname-wrapper">
      <h2>닉네임 설정</h2>
      <p>블로그 활동에 사용할 닉네임을 설정해주세요.</p>
      <form @submit.prevent="submitNickname">
        <div class="form-group">
          <label for="nickname">닉네임</label>
          <input
              id="nickname"
              v-model.trim="nickname"
              maxlength="15"
              minlength="2"
              pattern="^[a-zA-Z0-9가-힣_]+$"
              placeholder="2~15자의 영문, 숫자, 한글, _"
              required
              type="text"
          >
          <div v-if="inputError" class="error-message">{{ inputError }}</div>
        </div>

        <div v-if="serverError" class="error-message">{{ serverError }}</div>

        <button :disabled="isSubmitting" class="submit-btn" type="submit">
          {{ isSubmitting ? '저장 중...' : '닉네임 저장' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'SetNickname',
  data() {
    return {
      nickname: '',
      inputError: '',
      serverError: '',
      isSubmitting: false,
    };
  },
  methods: {
    validateInput() {
      this.inputError = '';
      if (!this.nickname) {
        this.inputError = '닉네임을 입력해주세요.';
        return false;
      }
      if (this.nickname.length < 2 || this.nickname.length > 15) {
        this.inputError = '닉네임은 2자 이상 15자 이하로 입력해주세요.';
        return false;
      }
      const pattern = /^[a-zA-Z0-9가-힣_]+$/;
      if (!pattern.test(this.nickname)) {
        this.inputError = '닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.';
        return false;
      }
      return true;
    },
    async submitNickname() {
      this.serverError = '';
      if (!this.validateInput()) {
        return;
      }

      this.isSubmitting = true;
      try {
        await authService.updateUserNickname(this.nickname);
        alert('닉네임이 성공적으로 설정되었습니다.');
        // 로컬 스토리지 사용자 정보도 업데이트 되었으므로 바로 이동
        this.$router.push('/'); // 설정 후 홈으로 이동
      } catch (error) {
        console.error('닉네임 설정 오류:', error);
        if (error.response && error.response.data && error.response.data.message) {
          // 백엔드에서 구체적인 에러 메시지를 보낸 경우 (예: 중복 닉네임)
          this.serverError = error.response.data.message;
        } else if (error.message) {
          this.serverError = error.message;
        } else {
          this.serverError = '닉네임 설정 중 오류가 발생했습니다. 다시 시도해주세요.';
        }
      } finally {
        this.isSubmitting = false;
      }
    },
  },
  mounted() {
    // 페이지 진입 시 현재 닉네임이 이미 있는지 확인하고 리다이렉트 로직 추가 가능
    const user = authService.getStoredUser();
    if (user && user.nickName) {
      console.log("이미 닉네임이 설정되어 있습니다. 홈으로 이동합니다.");
      this.$router.replace('/'); // 사용자가 뒤로가기로 이 페이지 다시 못 오게 replace 사용
    }
  }
};
</script>

<style scoped>
.set-nickname-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh; /* 최소 높이 */
  padding: 20px;
  background-color: #f5f5f5; /* 로그인 페이지와 유사한 배경 */
}

.set-nickname-wrapper {
  background-color: white;
  padding: 30px 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 450px;
  text-align: center;
}

h2 {
  margin-bottom: 10px;
  font-size: 22px;
  color: #333;
}

p {
  margin-bottom: 25px;
  color: #666;
  font-size: 14px;
}

.form-group {
  margin-bottom: 20px;
  text-align: left; /* 레이블, 인풋 왼쪽 정렬 */
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box; /* 패딩 포함 너비 계산 */
}

.error-message {
  color: red;
  font-size: 13px;
  text-align: left;
  margin-top: 5px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background-color: #007bff; /* 로그인 페이지와 유사한 스타일 */
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.submit-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}
</style>
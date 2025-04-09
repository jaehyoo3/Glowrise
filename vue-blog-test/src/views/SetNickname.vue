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
import {mapActions, mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import

export default {
  name: 'SetNickname',
  data() {
    return {
      nickname: '',          // 입력된 닉네임
      inputError: '',      // 입력 유효성 에러 메시지
      serverError: '',     // 서버 응답 에러 메시지
      isSubmitting: false, // 제출 중 상태
    };
  },
  computed: {
    // --- Vuex Getters 매핑 ---
    ...mapGetters(['isLoggedIn', 'nickName', 'isLoadingUser']), // 로그인 상태, 닉네임, 스토어 로딩 상태
    // ------------------------
  },
  watch: {
    // 스토어 로딩 완료 및 닉네임 상태 변경 감지
    isLoadingUser(loading) {
      if (!loading && this.isLoggedIn && this.nickName) {
        console.log("SetNickname Watcher: 스토어 로드 완료, 닉네임 존재 확인 -> 홈으로 이동");
        this.redirectToHomeIfNicknameExists();
      }
    },
    nickName(newNickname) {
      if (this.isLoggedIn && newNickname) {
        console.log("SetNickname Watcher: 스토어 닉네임 변경됨 -> 홈으로 이동");
        this.redirectToHomeIfNicknameExists();
      }
    }
  },
  methods: {
    // --- Vuex Actions 매핑 ---
    ...mapActions(['fetchCurrentUser']), // 사용자 정보 갱신용 액션
    // ------------------------

    // 입력값 유효성 검사 (기존과 동일)
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

    // 닉네임 제출
    async submitNickname() {
      this.serverError = '';
      // 입력 유효성 검사
      if (!this.validateInput()) {
        return;
      }

      this.isSubmitting = true;
      try {
        // authService 통해 닉네임 업데이트 API 호출 (변경 없음)
        await authService.updateUserNickname(this.nickname);
        alert('닉네임이 성공적으로 설정되었습니다.');

        // --- Vuex: 성공 후 스토어 상태 갱신 ---
        // fetchCurrentUser 액션을 호출하여 최신 사용자 정보(닉네임 포함)를 스토어에 반영
        await this.fetchCurrentUser();
        console.log("SetNickname: 스토어 사용자 정보 갱신 완료.");
        // ---------------------------------

        // 스토어 갱신 후 홈으로 이동
        this.$router.push('/');

      } catch (error) {
        console.error('닉네임 설정 오류:', error);
        // 에러 메시지 처리 (기존과 동일)
        if (error.response?.data?.message) {
          this.serverError = error.response.data.message;
        } else {
          this.serverError = '닉네임 설정 중 오류가 발생했습니다. 다시 시도해주세요.';
        }
      } finally {
        this.isSubmitting = false;
      }
    },

    // 닉네임 존재 시 홈으로 리디렉션하는 함수
    redirectToHomeIfNicknameExists() {
      // 로그인 상태이고 닉네임이 존재하면 홈으로 replace
      // replace를 사용하여 뒤로가기로 이 페이지에 다시 접근하는 것을 방지
      if (this.isLoggedIn && this.nickName) {
        console.log("SetNickname: 이미 닉네임이 설정되어 홈으로 이동합니다.");
        this.$router.replace('/');
      }
    }
  },
  mounted() {
    // --- Vuex: 마운트 시 스토어 상태 확인 ---
    // 스토어가 아직 로딩 중일 수 있으므로, 로딩 완료 후 또는 즉시 확인
    console.log("SetNickname: mounted hook. 스토어 닉네임 확인 시도.");
    // 스토어가 로딩 중이 아니고, 로그인 상태이며, 닉네임이 이미 있다면 리디렉션
    if (!this.isLoadingUser && this.isLoggedIn && this.nickName) {
      this.redirectToHomeIfNicknameExists();
    } else if (!this.isLoggedIn && !this.isLoadingUser) {
      // 로그인이 안 된 상태로 이 페이지에 접근했다면 홈으로 보냄
      console.log("SetNickname: 로그인되지 않은 상태입니다. 홈으로 이동합니다.");
      this.$router.replace('/');
    }
    // 스토어가 로딩 중이라면 watch에서 처리될 때까지 기다림
    // -----------------------------------
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
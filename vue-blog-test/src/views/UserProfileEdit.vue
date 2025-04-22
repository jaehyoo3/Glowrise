<template>
  <div class="user-profile-edit container">
    <h1>내 정보 수정</h1>
    <div v-if="isLoading" class="loading-container">
      <div class="loader"></div>
      <span>로딩 중...</span>
    </div>
    <div v-else-if="user" class="content-container">
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
          <small class="form-text">
            비밀번호를 변경하지 않으려면 비워두세요.
          </small>
        </div>

        <!-- 에러 메시지 표시 -->
        <div v-if="serverError" class="error-message">
          {{ serverError }}
        </div>

        <!-- 제출 버튼 -->
        <div class="form-actions">
          <router-link class="btn btn-cancel" to="/">취소</router-link>
          <button :disabled="isSubmitting" class="btn btn-save" type="submit">
            {{ isSubmitting ? '저장 중...' : '저장' }}
          </button>
        </div>
      </form>
    </div>
    <div v-else class="error-container">
      <h2>사용자 정보를 불러올 수 없습니다.</h2>
      <router-link class="btn btn-home" to="/">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<script>
import {mapActions, mapGetters, mapState} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import

export default {
  name: 'UserProfileEdit',
  data() {
    return {
      formData: { // 폼 데이터
        username: '', // 사용자 ID (표시용, 수정 불가)
        email: '',    // 이메일 (표시용, 수정 불가)
        nickName: '', // 닉네임 (수정 가능)
        password: '', // 새 비밀번호 (로컬 사용자만 입력 가능)
      },
      isLocalUser: false, // 로컬 계정 여부 (비밀번호 필드 표시 제어용)
      isSubmitting: false, // 폼 제출 중 상태
      serverError: '', // 서버 에러 메시지
    };
  },
  computed: {
    // --- Vuex State/Getters 매핑 ---
    ...mapState(['currentUser']), // 스토어의 currentUser 상태 직접 접근
    ...mapGetters(['isLoggedIn', 'userId', 'username', 'email', 'nickName', 'isLoadingUser']), // 필요한 getter들
    // 'site' 정보가 currentUser 객체 안에 있다고 가정
    siteType() {
      return this.currentUser?.site || 'UNKNOWN'; // 예: 'LOCAL', 'GOOGLE' 등
    }
  },
  watch: {
    // 스토어의 currentUser 상태 변경 감지하여 폼 데이터 업데이트
    currentUser: {
      handler(newUser) {
        if (newUser) {
          console.log("UserProfileEdit Watcher: 스토어 currentUser 변경 감지", newUser);
          this.formData.username = newUser.username || '';
          this.formData.email = newUser.email || '';
          // 닉네임이 없으면 username으로 초기 설정
          this.formData.nickName = newUser.nickName || newUser.username || '';
          this.formData.password = ''; // 비밀번호 필드는 항상 비움
          // 로컬 계정 여부 판단 (백엔드 응답 필드명 확인 필요)
          this.isLocalUser = this.siteType === 'LOCAL';
        } else {
          // 사용자 정보가 없어지면 (로그아웃 등) 폼 초기화
          this.resetForm();
        }
      },
      immediate: true, // 컴포넌트 생성 시 즉시 실행하여 초기값 설정
    },
    // 로그인 상태 감지 (로그아웃 시 리디렉션 등 처리)
    isLoggedIn(loggedIn) {
      if (!loggedIn && !this.isLoadingUser) {
        console.log("UserProfileEdit Watcher: 로그아웃됨 -> 홈으로 이동");
        alert("로그인이 필요합니다.");
        this.$router.replace('/'); // 로그인 안되어 있으면 접근 불가
      }
    }
  },
  methods: {
    // --- Vuex Actions 매핑 ---
    ...mapActions(['fetchCurrentUser']), // 사용자 정보 갱신용 액션

    // 폼 제출
    async submitForm() {
      if (this.isSubmitting) return;
      this.serverError = ''; // 이전 에러 메시지 초기화

      // 유효성 검사 (닉네임, 비밀번호 등) - 필요시 추가
      // 예: 닉네임 유효성 검사 (SetNickname과 유사하게)
      if (!this.validateNickname()) return;
      // 예: 로컬 사용자이고 비밀번호 입력 시 유효성 검사
      if (this.isLocalUser && this.formData.password && !this.validatePassword()) return;

      this.isSubmitting = true;
      try {
        // 업데이트할 데이터 준비
        const updatedData = {
          nickName: this.formData.nickName,
          // 비밀번호는 로컬 사용자이고 입력되었을 때만 포함
          password: (this.isLocalUser && this.formData.password) ? this.formData.password : undefined,
        };
        // undefined 필드는 JSON.stringify 시 제외됨

        // authService 통해 프로필 업데이트 API 호출 (변경 없음)
        // username은 경로 파라미터로 사용
        if (!this.username) throw new Error("사용자 username을 찾을 수 없습니다.");
        const updatedUserResponse = await authService.updateUserProfile(this.username, updatedData);

        // --- 로컬 스토리지 업데이트 (선택적이지만 유지 권장) ---
        // authService.setStoredUser를 사용하여 로컬 스토리지에도 반영
        // 백엔드 응답에서 최신 정보를 가져와 저장
        authService.setStoredUser({
          id: updatedUserResponse.id || this.userId, // 응답 또는 스토어 ID 사용
          username: updatedUserResponse.username || this.username,
          nickName: updatedUserResponse.nickName, // 응답에서 받은 닉네임
          email: updatedUserResponse.email || this.email,
          // site 등 다른 필요한 정보 포함
        });
        console.log("UserProfileEdit: 로컬 스토리지 사용자 정보 업데이트 완료.");
        // --------------------------------------------------

        // --- Vuex: 성공 후 스토어 상태 갱신 ---
        await this.fetchCurrentUser(); // 최신 사용자 정보로 스토어 업데이트
        console.log("UserProfileEdit: 스토어 사용자 정보 갱신 완료.");
        // ---------------------------------

        alert('정보가 성공적으로 수정되었습니다.');
        this.formData.password = ''; // 성공 후 비밀번호 필드 초기화
        // 필요하다면 홈 또는 다른 페이지로 이동
        // this.$router.push('/');

      } catch (error) {
        console.error('정보 수정 실패:', error);
        this.serverError = error.response?.data?.message || '정보 수정 중 오류가 발생했습니다.';
      } finally {
        this.isSubmitting = false;
      }
    },

    // 폼 초기화 함수 (로그아웃 또는 에러 시 사용 가능)
    resetForm() {
      this.formData.username = '';
      this.formData.email = '';
      this.formData.nickName = '';
      this.formData.password = '';
      this.isLocalUser = false;
      this.serverError = '';
    },

    // 닉네임 유효성 검사 (SetNickname과 유사)
    validateNickname() {
      const nickname = this.formData.nickName;
      if (!nickname || nickname.length < 2 || nickname.length > 15) {
        this.serverError = '닉네임은 2자 이상 15자 이하로 입력해주세요.';
        return false;
      }
      const pattern = /^[a-zA-Z0-9가-힣_]+$/;
      if (!pattern.test(nickname)) {
        this.serverError = '닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.';
        return false;
      }
      return true;
    },

    // 비밀번호 유효성 검사 (필요 시 추가)
    validatePassword() {
      const password = this.formData.password;
      if (!password) return true; // 비밀번호 변경 안 할 경우 유효
      const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&.])[A-Za-z\d@$!%*#?&.]{8,}$/;
      if (!passwordRegex.test(password)) {
        this.serverError = '비밀번호는 8자 이상이며, 영문, 숫자, 특수문자(@$!%*#?&.)를 포함해야 합니다.';
        return false;
      }
      return true;
    }
  },
  // mounted에서 로그인 상태 확인 (선택적)
  mounted() {
    // 스토어가 로딩되지 않았고, 로그인이 안되어 있다면 리디렉션
    if (!this.isLoadingUser && !this.isLoggedIn) {
      console.log("UserProfileEdit Mounted: 로그인되지 않음 -> 홈으로 이동");
      alert("로그인이 필요합니다.");
      this.$router.replace('/');
    }
  }
};
</script>

<style scoped>
.user-profile-edit {
  max-width: 680px;
  margin: 2rem auto;
  padding: 0 1rem;
}

h1 {
  font-size: 1.75rem;
  font-weight: 600;
  margin-bottom: 2rem;
  text-align: center;
  color: #333;
  letter-spacing: 0.5px;
}

h2 {
  font-size: 1.4rem;
  font-weight: 500;
  margin-bottom: 1.5rem;
  color: #444;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  text-align: center;
  color: #666;
}

.loader {
  width: 40px;
  height: 40px;
  border: 3px solid #eee;
  border-top: 3px solid #777;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.content-container {
  width: 100%;
}

.profile-form {
  background-color: #fff;
  padding: 2.5rem;
  border-radius: 4px;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.08);
  border: 1px solid #eaeaea;
}

.form-group {
  margin-bottom: 1.75rem;
}

.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #444;
  font-size: 0.95rem;
}

.form-control {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #ddd;
  border-radius: 3px;
  font-size: 0.95rem;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: #999;
  box-shadow: 0 0 0 2px rgba(153, 153, 153, 0.2);
}

.form-control:disabled {
  background-color: #f8f9fa;
  color: #777;
  cursor: not-allowed;
}

.form-text {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #777;
}

.error-message {
  background-color: #fff8f8;
  color: #e74c3c;
  padding: 0.8rem 1rem;
  margin-bottom: 1.5rem;
  border-radius: 3px;
  border-left: 3px solid #e74c3c;
  font-size: 0.9rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 2.5rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  border-radius: 3px;
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
  text-align: center;
  border: none;
}

.btn-save {
  background-color: #555;
  color: white;
}

.btn-save:hover {
  background-color: #444;
}

.btn-save:disabled {
  background-color: #999;
  cursor: not-allowed;
}

.btn-cancel {
  background-color: #f2f2f2;
  color: #555;
}

.btn-cancel:hover {
  background-color: #e8e8e8;
}

.btn-home {
  background-color: #555;
  color: white;
  display: inline-block;
  margin-top: 1rem;
}

.btn-home:hover {
  background-color: #444;
}

.error-container {
  text-align: center;
  padding: 3rem 1rem;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.08);
  border: 1px solid #eaeaea;
}

@media (max-width: 768px) {
  .user-profile-edit {
    max-width: 100%;
    padding: 0 1rem;
  }

  .profile-form {
    padding: 1.5rem;
  }

  .form-actions {
    flex-direction: column-reverse;
    gap: 0.5rem;
  }

  .btn {
    width: 100%;
  }
}
</style>
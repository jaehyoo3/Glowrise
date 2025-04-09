<template>
  <div class="user-profile-edit container mt-4">
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
import {mapActions, mapGetters, mapState} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import

export default {
  name: 'UserProfileEdit',
  data() {
    return {
      // --- 삭제: user 로컬 상태 ---
      formData: { // 폼 데이터
        username: '', // 사용자 ID (표시용, 수정 불가)
        email: '',    // 이메일 (표시용, 수정 불가)
        nickName: '', // 닉네임 (수정 가능)
        password: '', // 새 비밀번호 (로컬 사용자만 입력 가능)
        // 필요한 다른 필드 추가
      },
      isLocalUser: false, // 로컬 계정 여부 (비밀번호 필드 표시 제어용)
      // isLoading: true, // 로딩 상태는 스토어의 isLoadingUser 사용
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
    // ----------------------------
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
  // --- 삭제: created 훅 (loadUserData 호출 제거) ---
  // watch immediate 옵션으로 대체됨
  methods: {
    // --- Vuex Actions 매핑 ---
    ...mapActions(['fetchCurrentUser']), // 사용자 정보 갱신용 액션
    // ------------------------

    // --- 삭제: loadUserData 메서드 ---

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
        // alert('정보 수정에 실패했습니다: ' + this.serverError); // 필요한 경우 alert
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
      // ... (SetNickname의 validateInput 로직과 유사하게 구현) ...
      // 예:
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
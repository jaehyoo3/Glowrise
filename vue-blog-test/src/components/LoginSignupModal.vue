<template>
  <div class="modal-overlay" @click.self="closeModal">
    <div class="modal-content login-wrapper">
      <button class="close-button" @click="closeModal">&times;</button>
      <div class="login-header">
        <h1 class="blog-title">{{ isOAuthProfileCompletion ? '프로필 설정' : 'Glowrise' }}</h1>
        <p class="blog-subtitle">{{ isOAuthProfileCompletion ? '마지막 단계! 사용할 닉네임을 입력하세요.' : '로그인 또는 회원가입' }}</p>
      </div>

      <div class="login-form-container">
        <div v-if="!isOAuthProfileCompletion" class="login-tabs">
          <button
              :class="['tab-btn', { active: activeTab === 'login' }]"
              @click="activeTab = 'login'"
          >
            로그인
          </button>
          <button
              :class="['tab-btn', { active: activeTab === 'signup' }]"
              @click="activeTab = 'signup'"
          >
            회원가입
          </button>
        </div>

        <div v-if="activeTab === 'login' && !isOAuthProfileCompletion" class="login-form">
          <form @submit.prevent="handleLogin">
            <div class="form-group">
              <label for="modal-email">이메일</label>
              <input id="modal-email" v-model="loginForm.email" required type="email"/>
            </div>
            <div class="form-group">
              <label for="modal-password">비밀번호</label>
              <input id="modal-password" v-model="loginForm.password" required type="password"/>
            </div>
            <div v-if="loginError" class="error-message">{{ loginError }}</div>
            <button :disabled="isSubmitting" class="login-btn" type="submit">{{
                isSubmitting ? '로그인 중...' : '로그인'
              }}
            </button>
          </form>
          <div class="divider"><span>또는</span></div>
          <div class="social-login">
            <button class="social-btn naver-btn" @click="handleSocialLogin('naver')"><i class="fa-brands fa-neos"></i>
              네이버 로그인
            </button>
            <button class="social-btn google-btn" @click="handleSocialLogin('google')"><i
                class="fa-brands fa-google"></i> Google 로그인
            </button>
          </div>
        </div>

        <div v-if="activeTab === 'signup'" class="signup-form">
          <form @submit.prevent="handleSignup">
            <div class="form-group">
              <label for="modal-signup-email">이메일</label>
              <input id="modal-signup-email" v-model="signupForm.email" :disabled="isOAuthProfileCompletion" required
                     type="email"/>
              <small v-if="isOAuthProfileCompletion && signupForm.email">(SNS 계정 정보)</small>
            </div>
            <div class="form-group">
              <label for="modal-signup-username">사용자 이름</label>
              <input id="modal-signup-username" v-model="signupForm.username" :disabled="isOAuthProfileCompletion" required
                     type="text"/>
              <small v-if="isOAuthProfileCompletion && signupForm.username">(SNS 정보 기반)</small>
            </div>
            <div class="form-group">
              <label for="modal-signup-nickname">닉네임</label>
              <input id="modal-signup-nickname" ref="nicknameInput" v-model.trim="signupForm.nickname" maxlength="15" minlength="2"
                     pattern="^[a-zA-Z0-9가-힣_]+$" required type="text"/>
              <small>2~15자, 영문, 숫자, 한글, 밑줄(_)만 사용 가능</small>
              <div v-if="nicknameInputError" class="error-message nickname-error">{{ nicknameInputError }}</div>
            </div>

            <div v-if="!isOAuthProfileCompletion">
              <div class="form-group">
                <label for="modal-signup-password">비밀번호</label>
                <input id="modal-signup-password" v-model="signupForm.password" required type="password"/>
                <small>8자 이상, 영문, 숫자, 특수문자(@$!%*#?&.) 포함</small>
              </div>
              <div class="form-group">
                <label for="modal-signup-password-confirm">비밀번호 확인</label>
                <input id="modal-signup-password-confirm" v-model="signupForm.passwordConfirm" required
                       type="password"/>
              </div>
            </div>

            <div v-if="signupError" class="error-message server-error">{{ signupError }}</div>

            <button :disabled="isSubmitting" class="signup-btn" type="submit">
              {{
                isOAuthProfileCompletion ? (isSubmitting ? '저장 중...' : '프로필 완료') : (isSubmitting ? '가입 처리 중...' : '회원가입')
              }}
            </button>
          </form>

          <template v-if="!isOAuthProfileCompletion">
            <div class="divider"><span>또는</span></div>
            <div class="social-signup">
              <button class="social-btn naver-btn" @click="handleSocialLogin('naver')"><i class="fa-brands fa-neos"></i>
                네이버로 가입
              </button>
              <button class="social-btn google-btn" @click="handleSocialLogin('google')"><i
                  class="fa-brands fa-google"></i> Google로 가입
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import authService from '@/services/authService';
// Vuex 관련 import는 여기서는 직접 필요하지 않음 (이벤트 emit 후 부모에서 처리)

export default {
  name: 'LoginSignupModal',
  props: {
    initialTab: {
      type: String,
      default: 'login'
    },
    oauthCompletionData: { // OAuth 프로필 완료용 데이터 (email, oauthName)
      type: Object,
      default: null
    }
  },
  emits: ['close', 'login-success', 'profile-completed'], // 부모(NavBar)로 전달되는 이벤트
  data() {
    return {
      activeTab: 'login',
      loginForm: {email: '', password: ''},
      signupForm: {email: '', username: '', nickname: '', password: '', passwordConfirm: ''},
      loginError: '',
      signupError: '',
      nicknameInputError: '',
      isOAuthProfileCompletion: false,
      isSubmitting: false,
    };
  },
  created() {
    // Props 기반 초기 상태 설정
    this.activeTab = this.initialTab;
    if (this.oauthCompletionData) {
      console.log("LoginSignupModal: OAuth 완료 모드로 생성됨", this.oauthCompletionData);
      this.isOAuthProfileCompletion = true;
      this.activeTab = 'signup'; // 프로필 완료를 위해 signup 탭 강제
      // NavBar로부터 받은 데이터로 폼 미리 채우기
      this.signupForm.email = this.oauthCompletionData.email || '';
      this.signupForm.username = this.oauthCompletionData.oauthName || this.oauthCompletionData.email?.split('@')[0] || '';
      this.signupForm.nickname = ''; // 닉네임은 사용자가 입력

      // DOM 업데이트 후 닉네임 입력 필드에 포커스
      this.$nextTick(() => {
        if (this.$refs.nicknameInput) {
          this.$refs.nicknameInput.focus();
        }
      });
    } else {
      console.log("LoginSignupModal: 일반 모드로 생성됨, initialTab:", this.initialTab);
    }
  },
  methods: {
    closeModal() {
      this.$emit('close'); // 부모에게 close 이벤트 전달
    },
    async handleLogin() {
      this.loginError = '';
      if (!this.loginForm.email || !this.loginForm.password) {
        this.loginError = '이메일과 비밀번호를 모두 입력해주세요.';
        return;
      }
      this.isSubmitting = true;
      console.log("LoginSignupModal: 로그인 시도:", this.loginForm.email);
      try {
        // authService.login 호출은 그대로 유지
        const response = await authService.login(this.loginForm.email, this.loginForm.password);
        console.log('LoginSignupModal: 로그인 성공:', response);

        // --- 변경 없음: 로그인 성공 이벤트만 발생시키고, 스토어 업데이트는 NavBar에서 처리 ---
        this.$emit('login-success');
        // 부모(NavBar)의 handleAuthSuccess에서 모달 닫고 스토어 액션 디스패치
        // ---------------------------------------------------------------------
      } catch (error) {
        console.error('LoginSignupModal: 로그인 에러:', error.response?.data || error.message);
        this.loginError = error.response?.data?.message || '이메일 또는 비밀번호가 올바르지 않습니다.';
      } finally {
        this.isSubmitting = false;
      }
    },
    async handleSignup() {
      this.signupError = '';
      this.nicknameInputError = '';

      if (!this.validateNicknameInput()) {
        return;
      }

      this.isSubmitting = true;

      try {
        if (this.isOAuthProfileCompletion) {
          // --- OAuth 프로필 완료 로직 ---
          console.log("LoginSignupModal: OAuth 프로필 완료 시도. 닉네임:", this.signupForm.nickname);
          // 닉네임 업데이트 API 호출
          await authService.updateUserNickname(this.signupForm.nickname);
          console.log('LoginSignupModal: 닉네임 업데이트 성공');
          alert('닉네임이 성공적으로 설정되었습니다.');

          // --- 변경 없음: 프로필 완료 이벤트 발생시키고, 스토어 업데이트는 NavBar에서 처리 ---
          this.$emit('profile-completed');
          // 부모(NavBar)의 handleAuthSuccess에서 모달 닫고 스토어 액션 디스패치
          // ---------------------------------------------------------------------

        } else {
          // --- 일반 회원가입 로직 ---
          console.log("LoginSignupModal: 일반 회원가입 시도:", this.signupForm.email);

          // 비밀번호 검증 등 (기존과 동일)
          if (this.signupForm.password !== this.signupForm.passwordConfirm) {
            this.signupError = '비밀번호가 일치하지 않습니다.';
            this.isSubmitting = false;
            return;
          }
          const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&.])[A-Za-z\d@$!%*#?&.]{8,}$/;
          if (!passwordRegex.test(this.signupForm.password)) {
            this.signupError = '비밀번호는 8자 이상이며, 영문, 숫자, 특수문자(@$!%*#?&.)를 포함해야 합니다.';
            this.isSubmitting = false;
            return;
          }

          // 회원가입 API 호출
          await authService.signup({
            email: this.signupForm.email,
            username: this.signupForm.username,
            nickName: this.signupForm.nickname,
            password: this.signupForm.password
          });

          console.log('LoginSignupModal: 일반 회원가입 성공');
          alert('회원가입이 완료되었습니다. 로그인 탭에서 로그인해주세요.');

          // 로그인 탭으로 전환 및 이메일 자동 완성 (기존과 동일)
          this.activeTab = 'login';
          this.loginForm.email = this.signupForm.email;
          this.loginForm.password = '';
          this.signupForm = {email: '', username: '', nickname: '', password: '', passwordConfirm: ''};
        }
      } catch (error) {
        console.error('LoginSignupModal: 회원가입/프로필 완료 에러:', error.response?.data || error.message);
        this.signupError = error.response?.data?.message || (this.isOAuthProfileCompletion ? '닉네임 저장 중 오류가 발생했습니다.' : '회원가입 중 오류가 발생했습니다.');
        if (this.signupError.includes('닉네임')) {
          this.nicknameInputError = this.signupError;
          this.signupError = '';
          if (this.$refs.nicknameInput) this.$refs.nicknameInput.focus();
        }
      } finally {
        this.isSubmitting = false;
      }
    },
    validateNicknameInput() {
      // 닉네임 유효성 검사 로직 (기존과 동일)
      const nickname = this.signupForm.nickname;
      let errorMsg = '';
      if (!nickname) {
        errorMsg = '닉네임을 입력해주세요.';
      } else if (nickname.length < 2 || nickname.length > 15) {
        errorMsg = '닉네임은 2자 이상 15자 이하여야 합니다.';
      } else {
        const pattern = /^[a-zA-Z0-9가-힣_]+$/;
        if (!pattern.test(nickname)) {
          errorMsg = '닉네임은 영문, 숫자, 한글, 밑줄(_)만 사용 가능합니다.';
        }
      }
      this.nicknameInputError = errorMsg;
      if (errorMsg && this.$refs.nicknameInput) {
        this.$refs.nicknameInput.focus();
      }
      return !errorMsg;
    },
    handleSocialLogin(provider) {
      // 소셜 로그인 로직 (기존과 동일)
      console.log(`LoginSignupModal: ${provider} 소셜 로그인/가입 시작`);
      window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    }
  },
  watch: {
    // OAuth 데이터 변경 감지 로직 (기존과 동일)
    oauthCompletionData(newData) {
      console.log("LoginSignupModal: oauthCompletionData prop 변경 감지", newData);
      if (newData) {
        this.isOAuthProfileCompletion = true;
        this.activeTab = 'signup';
        this.signupForm.email = newData.email || '';
        this.signupForm.username = newData.oauthName || newData.email?.split('@')[0] || '';
        this.signupForm.nickname = '';
        this.loginError = '';
        this.signupError = '';
        this.nicknameInputError = '';
        this.$nextTick(() => {
          if (this.$refs.nicknameInput) this.$refs.nicknameInput.focus();
        });
      }
    },
    // 탭 변경 시 에러 클리어 로직 (기존과 동일)
    activeTab(newTab) {
      console.log("LoginSignupModal: 탭 변경됨 ->", newTab);
      if (newTab === 'login') {
        this.signupError = '';
        this.nicknameInputError = '';
      } else {
        this.loginError = '';
      }
    }
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000; /* NavBar보다 위에 오도록 */
}

.modal-content {
  position: relative; /* 닫기 버튼 기준점 */
  /* login-wrapper 스타일 재활용 */
}

.close-button {
  position: absolute;
  top: 10px;
  right: 15px;
  font-size: 1.8rem;
  font-weight: bold;
  color: #aaa;
  background: none;
  border: none;
  cursor: pointer;
  line-height: 1;
}

.close-button:hover {
  color: #333;
}

/* LoginView의 스타일 상속 또는 여기에 복사 */
.login-wrapper {
  background-color: white;
  padding: 30px 40px;
  border-radius: 8px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  width: 90%;
  max-width: 450px; /* 모달 최대 너비 조정 */
  box-sizing: border-box;
}

/* ... LoginView.vue의 <style scoped> 내용 대부분 복사 ... */
/* (필요 시 모달에 맞게 약간의 스타일 조정) */
.login-header {
  text-align: center;
  margin-bottom: 25px;
}

.blog-title {
  font-size: 1.6rem;
  font-weight: 700;
  color: #343a40;
  margin-bottom: 0.4rem;
}

.blog-subtitle {
  font-size: 0.95rem;
  color: #6c757d;
  min-height: 1.4em;
  line-height: 1.4;
}

.login-tabs {
  display: flex;
  border-bottom: 1px solid #dee2e6;
  margin-bottom: 20px;
}

.tab-btn {
  flex: 1;
  background: none;
  border: none;
  padding: 10px;
  font-size: 1rem;
  cursor: pointer;
  color: #6c757d;
  border-bottom: 3px solid transparent;
  margin-bottom: -1px;
  transition: color 0.2s ease, border-color 0.2s ease;
}

.tab-btn:hover {
  color: #495057;
}

.tab-btn.active {
  color: #007bff;
  border-bottom-color: #007bff;
  font-weight: 600;
}

.form-group {
  margin-bottom: 0.9rem;
  text-align: left;
}

.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 500;
  color: #495057;
  margin-bottom: 5px;
}

.form-group input {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  box-sizing: border-box;
}

input:disabled {
  background-color: #e9ecef;
  cursor: not-allowed;
  opacity: 0.7;
}

.form-group small {
  display: block;
  margin-top: 4px;
  font-size: 0.75rem;
  color: #6c757d;
}

.error-message {
  color: #dc3545;
  font-size: 0.8rem;
  margin-top: 4px;
  min-height: 1.1em;
}

.error-message.nickname-error {
  margin-bottom: -5px;
}

/* 닉네임 에러 위치 조정 */
.error-message.server-error {
  text-align: center;
  margin-top: 0.5rem;
  margin-bottom: 0.8rem;
}

.login-btn, .signup-btn {
  width: 100%;
  padding: 11px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.login-btn:hover:not(:disabled), .signup-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.login-btn:disabled, .signup-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.65;
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider span {
  background-color: white;
  padding: 0 10px;
  color: #6c757d;
  font-size: 0.8rem;
  position: relative;
  z-index: 1;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  border-top: 1px solid #dee2e6;
  z-index: 0;
}

.social-login, .social-signup {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 15px;
}

.social-btn {
  padding: 10px;
  border: none;
  border-radius: 5px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  color: white;
  text-align: center;
  transition: opacity 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.social-btn i {
  font-size: 1.1em;
}

.social-btn:hover {
  opacity: 0.9;
}

.naver-btn {
  background-color: #03c75a;
}

.google-btn {
  background-color: #4285f4;
}
</style>
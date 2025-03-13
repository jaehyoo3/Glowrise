<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-header">
        <h1 class="blog-title">Blog Platform</h1>
        <p class="blog-subtitle">나만의 블로그를 시작해보세요</p>
      </div>

      <div class="login-form-container">
        <div class="login-tabs">
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

        <!-- 로그인 폼 -->
        <div v-if="activeTab === 'login'" class="login-form">
          <form @submit.prevent="handleLogin">
            <div class="form-group">
              <label for="email">이메일</label>
              <input
                  type="email"
                  id="email"
                  v-model="loginForm.email"
                  placeholder="이메일을 입력하세요"
                  required
              >
            </div>

            <div class="form-group">
              <label for="password">비밀번호</label>
              <input
                  type="password"
                  id="password"
                  v-model="loginForm.password"
                  placeholder="비밀번호를 입력하세요"
                  required
              >
            </div>

            <div class="remember-me">
              <input type="checkbox" id="remember" v-model="rememberMe">
              <label for="remember">로그인 상태 유지</label>
            </div>

            <div v-if="loginError" class="error-message">
              {{ loginError }}
            </div>

            <button type="submit" class="login-btn">로그인</button>
          </form>

          <div class="divider">
            <span>또는</span>
          </div>

          <div class="social-login">
            <button @click="handleSocialLogin('naver')" class="social-btn naver-btn">
              네이버 계정으로 로그인
            </button>
            <button @click="handleSocialLogin('google')" class="social-btn google-btn">
              Google 계정으로 로그인
            </button>
          </div>
        </div>

        <!-- 회원가입 폼 -->
        <div v-if="activeTab === 'signup'" class="signup-form">
          <form @submit.prevent="handleSignup">
            <div class="form-group">
              <label for="signup-email">이메일</label>
              <input
                  type="email"
                  id="signup-email"
                  v-model="signupForm.email"
                  placeholder="이메일을 입력하세요"
                  required
              >
            </div>

            <div class="form-group">
              <label for="signup-username">사용자 이름</label>
              <input
                  type="text"
                  id="signup-username"
                  v-model="signupForm.username"
                  placeholder="사용자 이름을 입력하세요"
                  required
              >
            </div>

            <div class="form-group">
              <label for="signup-nickname">닉네임</label>
              <input
                  type="text"
                  id="signup-nickname"
                  v-model="signupForm.nickname"
                  placeholder="블로그에서 사용할 닉네임을 입력하세요"
                  required
              >
            </div>

            <div class="form-group">
              <label for="signup-password">비밀번호</label>
              <input
                  type="password"
                  id="signup-password"
                  v-model="signupForm.password"
                  placeholder="비밀번호를 입력하세요"
                  required
              >
              <small>8자 이상, 영문, 숫자, 특수문자 포함</small>
            </div>

            <div class="form-group">
              <label for="signup-password-confirm">비밀번호 확인</label>
              <input
                  type="password"
                  id="signup-password-confirm"
                  v-model="signupForm.passwordConfirm"
                  placeholder="비밀번호를 다시 입력하세요"
                  required
              >
            </div>

            <div v-if="signupError" class="error-message">
              {{ signupError }}
            </div>

            <button type="submit" class="signup-btn">회원가입</button>
          </form>

          <div class="divider">
            <span>또는</span>
          </div>

          <div class="social-signup">
            <button @click="handleSocialLogin('naver')" class="social-btn naver-btn">
              네이버 계정으로 회원가입
            </button>
            <button @click="handleSocialLogin('google')" class="social-btn google-btn">
              Google 계정으로 회원가입
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'LoginView',
  data() {
    return {
      activeTab: 'login',
      loginForm: { email: '', password: '' },
      signupForm: { email: '', username: '', nickname: '', password: '', passwordConfirm: '' },
      rememberMe: false,
      loginError: '',
      signupError: ''
    };
  },
  methods: {
    async handleLogin() {
      try {
        this.loginError = '';
        const response = await authService.login(this.loginForm.email, this.loginForm.password);
        console.log('로그인 성공:', response);
        console.log('Stored User after login:', authService.getStoredUser());
        this.$emit('login-success');
        this.$router.push('/');
      } catch (error) {
        console.error('로그인 에러:', error);
        this.loginError = error.response?.data?.message || '로그인에 실패했습니다.';
      }
    },

    async handleSignup() {
      if (this.signupForm.password !== this.signupForm.passwordConfirm) {
        this.signupError = '비밀번호가 일치하지 않습니다.';
        return;
      }
      const passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/;
      if (!passwordRegex.test(this.signupForm.password)) {
        this.signupError = '비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.';
        return;
      }
      try {
        this.signupError = '';
        await authService.signup({
          email: this.signupForm.email,
          username: this.signupForm.username,
          nickName: this.signupForm.nickname,
          password: this.signupForm.password
        });
        this.activeTab = 'login';
        this.loginForm.email = this.signupForm.email;
        this.loginForm.password = '';
        alert('회원가입이 완료되었습니다. 로그인해 주세요.');
        this.signupForm = { email: '', username: '', nickname: '', password: '', passwordConfirm: '' };
      } catch (error) {
        console.error('회원가입 에러:', error);
        this.signupError = error.response?.data?.message || '회원가입에 실패했습니다.';
      }
    },

    handleSocialLogin(provider) {
      window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    }
  }
};
</script>

<style scoped>
/* 기존 스타일 유지 */
</style>
<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f6f7;
  padding: 20px;
}

.login-wrapper {
  width: 100%;
  max-width: 500px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 30px 20px;
  background-color: #03C75A; /* 네이버 색상 */
  color: white;
}

.blog-title {
  font-size: 28px;
  margin-bottom: 5px;
  font-weight: bold;
}

.blog-subtitle {
  font-size: 16px;
  opacity: 0.9;
}

.login-form-container {
  padding: 20px;
}

.login-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #e5e5e5;
}

.tab-btn {
  flex: 1;
  padding: 15px;
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #888;
  font-weight: 500;
}

.tab-btn.active {
  color: #03C75A;
  border-bottom: 2px solid #03C75A;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group small {
  color: #888;
  font-size: 12px;
  margin-top: 5px;
  display: block;
}

.remember-me {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.remember-me input {
  margin-right: 8px;
}

.error-message {
  color: #e74c3c;
  margin-bottom: 15px;
  font-size: 14px;
}

.login-btn, .signup-btn {
  width: 100%;
  padding: 12px;
  background-color: #03C75A;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 15px;
}

.login-btn:hover, .signup-btn:hover {
  background-color: #02b350;
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 45%;
  height: 1px;
  background-color: #e5e5e5;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  background-color: white;
  padding: 0 10px;
  color: #888;
  position: relative;
  z-index: 1;
}

.social-login, .social-signup {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-btn {
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}

.naver-btn {
  background-color: #03C75A;
  color: white;
}

.google-btn {
  background-color: white;
  border: 1px solid #ddd;
  color: #333;
}

@media (max-width: 576px) {
  .login-wrapper {
    max-width: 100%;
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
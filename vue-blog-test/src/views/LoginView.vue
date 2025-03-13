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
    // 일반 로그인 처리
    async handleLogin() {
      try {
        this.loginError = '';
        const response = await authService.login(this.loginForm.email, this.loginForm.password);
        console.log('로그인 성공:', response);
        console.log('Stored User after login:', authService.getStoredUser());
        this.$emit('login-success');
        this.$router.push('/'); // 홈으로 이동
      } catch (error) {
        console.error('로그인 에러:', error);
        this.loginError = error.response?.data?.message || '로그인에 실패했습니다.';
      }
    },

    // 회원가입 처리
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

    // 소셜 로그인 (OAuth2) 처리
    handleSocialLogin(provider) {
      console.log(`Initiating ${provider} login`);
      window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.login-wrapper {
  background-color: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.blog-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.blog-subtitle {
  font-size: 14px;
  color: #666;
}

.login-tabs {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.tab-btn {
  background: none;
  border: none;
  padding: 10px;
  font-size: 16px;
  cursor: pointer;
  color: #666;
}

.tab-btn.active {
  color: #007bff;
  border-bottom: 2px solid #007bff;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group small {
  font-size: 12px;
  color: #666;
}

.remember-me {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.remember-me label {
  margin-left: 5px;
  font-size: 14px;
  color: #666;
}

.error-message {
  color: red;
  font-size: 14px;
  margin-bottom: 15px;
}

.login-btn, .signup-btn {
  width: 100%;
  padding: 10px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.login-btn:hover, .signup-btn:hover {
  background-color: #0056b3;
}

.divider {
  text-align: center;
  margin: 20px 0;
  position: relative;
}

.divider span {
  background-color: white;
  padding: 0 10px;
  color: #666;
  font-size: 14px;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  border-top: 1px solid #ddd;
  z-index: -1;
}

.social-login, .social-signup {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-btn {
  padding: 10px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  color: white;
}

.naver-btn {
  background-color: #03c75a;
}

.naver-btn:hover {
  background-color: #02b050;
}

.google-btn {
  background-color: #4285f4;
}

.google-btn:hover {
  background-color: #357abd;
}
</style>
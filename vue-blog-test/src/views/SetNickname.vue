<template>
  <div class="set-nickname-container">
    <div class="set-nickname-wrapper">
      <div class="nickname-header">
        <h2 class="nickname-title">닉네임 설정</h2>
        <p class="nickname-subtitle">블로그 활동에 사용할 닉네임을 설정해주세요.</p>
      </div>

      <form @submit.prevent="submitNickname">
        <div class="form-group">
          <label for="nickname">닉네임</label>
          <input
              id="nickname"
              v-model.trim="nickname"
              maxlength="15"
              minlength="2"
              pattern="^[a-zA-Z0-9가-힣_]+$"
              placeholder="닉네임 입력"
              required
              type="text"
          >
          <small class="info-text">2~15자, 영문, 숫자, 한글, 밑줄(_)만 사용 가능</small>
          <div v-if="inputError" class="error-message">{{ inputError }}</div>
        </div>

        <div v-if="serverError" class="error-message server-error">{{ serverError }}</div>

        <button :disabled="isSubmitting" class="primary-btn" type="submit">
          {{ isSubmitting ? '저장 중...' : '닉네임 저장' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
:root {
  --primary-color: #2d3748;
  --accent-color: #4a5568;
  --light-gray: #e2e8f0;
  --medium-gray: #a0aec0;
  --dark-gray: #4a5568;
  --error-color: #e53e3e;
  --border-radius: 4px;
  --shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  --transition: all 0.2s ease;
}

.set-nickname-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
  padding: 20px;
  background-color: #f8fafc;
}

.set-nickname-wrapper {
  background-color: white;
  padding: 32px;
  border-radius: var(--border-radius);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 480px;
  box-sizing: border-box;
}

.nickname-header {
  text-align: center;
  margin-bottom: 24px;
}

.nickname-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.nickname-subtitle {
  font-size: 16px;
  color: var(--accent-color);
  line-height: 1.4;
  margin: 0;
}

.form-group {
  margin-bottom: 20px;
  text-align: left;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--dark-gray);
  margin-bottom: 6px;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--light-gray);
  border-radius: var(--border-radius);
  font-size: 15px;
  box-sizing: border-box;
  transition: var(--transition);
}

.form-group input:focus {
  outline: none;
  border-color: var(--dark-gray);
  box-shadow: 0 0 0 2px rgba(74, 85, 104, 0.1);
}

.info-text {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--medium-gray);
}

.error-message {
  color: var(--error-color);
  font-size: 13px;
  margin-top: 4px;
  font-weight: 500;
}

.error-message.server-error {
  text-align: center;
  margin: 12px 0;
}

.primary-btn {
  width: 100%;
  padding: 12px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  text-align: center;
  letter-spacing: -0.3px;
  margin-top: 8px;
}

.primary-btn:hover:not(:disabled) {
  background-color: #1a202c;
}

.primary-btn:disabled {
  background-color: var(--medium-gray);
  cursor: not-allowed;
}

/* 반응형 조정 */
@media (max-width: 576px) {
  .set-nickname-wrapper {
    padding: 24px;
  }
}
</style>
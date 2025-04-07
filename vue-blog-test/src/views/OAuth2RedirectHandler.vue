<template>
  <div>로그인 처리 중입니다... 잠시만 기다려주세요.</div>
</template>

<script>
import authService from '@/services/authService';

export default {
  name: 'OAuth2RedirectHandler',
  async mounted() {
    console.log('OAuth2RedirectHandler: 처리 시작.');
    try {
      // ... (accessToken, refreshToken 추출 로직) ...
      const urlParams = new URLSearchParams(window.location.search);
      const accessToken = urlParams.get('accessToken');
      const refreshToken = urlParams.get('refreshToken');

      if (accessToken && refreshToken) {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        const userData = await authService.getCurrentUser(); // 최신 정보 가져오기

        if (!userData) throw new Error('사용자 정보 로드 실패');

        const userId = userData.id || userData.userId;
        const username = userData.username;
        const nickName = userData.nickName;
        const userEmail = userData.email || '';
        const oauthName = userData.name || ''; // 백엔드 DTO에 name 필드 필요

        if (userId === undefined || username === undefined) {
          throw new Error("필수 사용자 정보(ID/Username) 누락");
        }

        // 로컬 스토리지에도 일단 저장
        authService.setStoredUser({id: userId, username: username, nickName: nickName, email: userEmail});

        if (!nickName || nickName.trim() === '') {
          // 닉네임 없으면 sessionStorage에 정보 저장 후 홈으로
          console.log('OAuth2RedirectHandler: 닉네임 없음. sessionStorage 설정 후 홈으로 이동.');
          sessionStorage.setItem('oauth_profile_completion', JSON.stringify({
            email: userEmail,
            oauthName: oauthName // 이름 정보 전달
          }));
          this.$router.push('/'); // 홈으로 리다이렉션
        } else {
          // 닉네임 있으면 홈으로
          console.log('OAuth2RedirectHandler: 닉네임 있음. 홈으로 이동.');
          sessionStorage.removeItem('oauth_profile_completion'); // 플래그 제거
          this.$router.push('/'); // 홈으로 리다이렉션
        }
      } else {
        throw new Error('리다이렉션 URL에 토큰 없음');
      }
    } catch (error) {
      console.error('OAuth2 처리 오류:', error);
      sessionStorage.removeItem('oauth_profile_completion');
      await authService.logout(); // 오류 시 클린업
      this.$router.push('/login'); // 오류 시 로그인 페이지로 (주의: /login 라우트 없음) -> 홈으로 변경?
      // alert('로그인 처리 중 오류 발생');
      // this.$router.push('/'); // 또는 홈으로 보내기
    }
  },
};
</script>

<style scoped>
/* 간단한 스타일링 예시 */
div {
  padding: 50px;
  text-align: center;
  font-size: 1.1em;
  color: #666;
}
</style>
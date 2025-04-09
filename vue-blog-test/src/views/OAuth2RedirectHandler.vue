<template>
  <div>로그인 처리 중입니다... 잠시만 기다려주세요.</div>
</template>

<script>
import {mapActions} from 'vuex'; // Vuex 액션 매핑 헬퍼 import
import authService from '@/services/authService';

export default {
  name: 'OAuth2RedirectHandler',
  methods: {
    // --- Vuex 액션 매핑 ---
    ...mapActions(['fetchCurrentUser']),
    // ------------------------
  },
  async mounted() {
    console.log('OAuth2RedirectHandler: 처리 시작.');
    try {
      // URL에서 토큰 추출
      const urlParams = new URLSearchParams(window.location.search);
      const accessToken = urlParams.get('accessToken');
      const refreshToken = urlParams.get('refreshToken');

      if (accessToken && refreshToken) {
        // 1. 토큰을 로컬 스토리지에 저장 (authService 및 인터셉터가 사용)
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        console.log('OAuth2RedirectHandler: 토큰 저장 완료.');

        // 2. 최신 사용자 정보 가져오기 (토큰 설정 후 API 호출)
        // authService.getCurrentUser()는 내부적으로 /api/users/me 호출
        // 성공 시 사용자 정보 반환, 실패 시 에러 throw (authService 수정 내용 반영)
        const userData = await authService.getCurrentUser();
        console.log('OAuth2RedirectHandler: 사용자 정보 로드 결과:', userData);

        if (!userData) throw new Error('사용자 정보 로드 실패 (null 반환)');

        // 사용자 정보에서 필요한 값 추출 (백엔드 DTO 필드명 확인)
        const userId = userData.id || userData.userId;
        const username = userData.username;
        const nickName = userData.nickName; // 닉네임 필드
        const userEmail = userData.email || '';
        const oauthName = userData.name || ''; // OAuth 제공자로부터 받은 이름 등 (백엔드 전달 확인)

        if (userId === undefined || username === undefined) {
          throw new Error("필수 사용자 정보(ID/Username) 누락");
        }

        // 3. 로컬 스토리지에도 사용자 정보 저장 (선택적, 빠른 접근용)
        authService.setStoredUser({id: userId, username: username, nickName: nickName, email: userEmail});
        console.log('OAuth2RedirectHandler: 로컬 스토리지 사용자 정보 저장.');

        // --- 4. Vuex 스토어 상태 갱신 ---
        // fetchCurrentUser 액션을 디스패치하여 스토어 상태 업데이트
        // 이 액션은 내부적으로 getCurrentUser를 다시 호출하고 블로그 정보도 가져올 수 있음
        await this.fetchCurrentUser();
        console.log('OAuth2RedirectHandler: Vuex 스토어 상태 갱신 완료.');
        // ------------------------------

        // 5. 닉네임 유무에 따른 리디렉션 처리
        if (!nickName || nickName.trim() === '') {
          // 닉네임 없으면 -> 프로필 완료 필요 플래그 설정 후 홈으로
          console.log('OAuth2RedirectHandler: 닉네임 없음. sessionStorage 설정 후 홈으로 이동.');
          sessionStorage.setItem('oauth_profile_completion', JSON.stringify({
            email: userEmail,
            oauthName: oauthName // 닉네임 설정 시 참고할 이름 정보
          }));
          this.$router.push('/'); // 홈으로 리다이렉션 (NavBar에서 sessionStorage 확인 후 모달 표시)
        } else {
          // 닉네임 있으면 -> 바로 홈으로
          console.log('OAuth2RedirectHandler: 닉네임 있음. 홈으로 이동.');
          sessionStorage.removeItem('oauth_profile_completion'); // 혹시 있을지 모를 플래그 제거
          this.$router.push('/'); // 홈으로 리다이렉션
        }
      } else {
        // URL에 토큰 파라미터가 없는 경우
        throw new Error('리다이렉션 URL에 토큰 없음');
      }
    } catch (error) {
      // 전체 프로세스 중 에러 발생 시
      console.error('OAuth2 처리 오류:', error);
      sessionStorage.removeItem('oauth_profile_completion'); // 플래그 제거
      // authService.logout() 호출하여 로컬 스토리지 정리 시도
      try {
        await authService.logout();
        // Vuex 상태 클리어도 필요 -> logoutAndClear 액션 디스패치
        this.$store.dispatch('logoutAndClear'); // 스토어 직접 접근
      } catch (logoutError) {
        console.error("OAuth 오류 처리 중 로그아웃 실패:", logoutError);
      } finally {
        // 오류 발생 시 홈으로 리디렉션 (로그인 페이지가 별도 없으므로)
        alert('로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.');
        this.$router.push('/');
      }
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
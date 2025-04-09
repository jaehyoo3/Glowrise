// src/store/index.js
import {createStore} from 'vuex';
import authService from '@/services/authService'; // authService import

export default createStore({
    // 상태 (State): 애플리케이션의 데이터
    state: {
        currentUser: null, // 현재 로그인한 사용자 정보
        userBlog: null,    // 현재 사용자의 블로그 정보
        isLoadingUser: false, // 사용자 정보 로딩 상태
        isLoadingBlog: false, // 블로그 정보 로딩 상태
    },

    // 뮤테이션 (Mutations): 상태를 동기적으로 변경하는 함수 (Commit으로 호출)
    mutations: {
        SET_LOADING_USER(state, isLoading) {
            state.isLoadingUser = isLoading;
        },
        SET_LOADING_BLOG(state, isLoading) {
            state.isLoadingBlog = isLoading;
        },

        SET_CURRENT_USER(state, user) {
            console.log("Mutation SET_CURRENT_USER receives:", user); // 전달받은 데이터 확인
            if (user && user.id) {
                state.currentUser = {...user};
            } else {
                state.currentUser = null;
            }

            // 최종 state.currentUser 값을 기반으로 로컬 스토리지 업데이트
            authService.setStoredUser(state.currentUser);
        },
        SET_USER_BLOG(state, blog) {
            state.userBlog = blog;
        },
        // 로그아웃 또는 초기화 시 상태 클리어
        CLEAR_USER_DATA(state) {
            state.currentUser = null;
            state.userBlog = null;
            // 로컬 스토리지 사용자 정보도 클리어 (authService.logout에서 처리될 수도 있음)
            // authService.setStoredUser(null); // 필요시 추가
        }
    },

    // 액션 (Actions): 비동기 작업 처리, 여러 뮤테이션 커밋 가능 (Dispatch로 호출)
    actions: {
        // 앱 로드 또는 로그인 후 사용자 정보 가져오기
        async fetchCurrentUser({commit, state, dispatch}) {
            // 이미 로드 중이거나 데이터가 있으면 중복 방지 (선택적 개선)
            if (state.isLoadingUser) return;
            // 토큰 유무 확인은 앱 시작 시 수행, 여기서는 API 호출 시도
            commit('SET_LOADING_USER', true);
            try {
                console.log('[Store Action] Fetching current user...');
                const user = await authService.getCurrentUser(); // /api/users/me 호출
                commit('SET_CURRENT_USER', user);
                console.log('[Store Action] Current user fetched:', user);
                // 사용자 정보 로드 성공 & 블로그 정보 아직 없으면 블로그 정보 가져오기 시도
                if (user && !state.userBlog) {
                    await dispatch('fetchUserBlog'); // fetchUserBlog 액션 호출
                } else if (!user) {
                    // 사용자를 가져오지 못했다면 블로그 정보도 초기화
                    commit('SET_USER_BLOG', null);
                }
            } catch (error) {
                console.error("[Store Action] 현재 사용자 정보 가져오기 실패:", error);
                // 에러 발생 시 상태 초기화 (401 등은 인터셉터가 처리 후 재시도될 수 있음)
                // 최종 실패 시 여기로 올 수 있음
                commit('CLEAR_USER_DATA'); // 사용자 관련 모든 상태 초기화
                // authService.clearAuthData(); // 필요시 로컬 스토리지도 여기서 클리어
            } finally {
                commit('SET_LOADING_USER', false);
            }
        },

        // 사용자 블로그 정보 가져오기
        async fetchUserBlog({commit, state}) {
            // 사용자 정보가 없거나, 이미 블로그 정보가 있거나, 로딩 중이면 중복 방지
            if (!state.currentUser || state.userBlog || state.isLoadingBlog) return;

            commit('SET_LOADING_BLOG', true);
            try {
                console.log('[Store Action] Fetching user blog...');
                const blog = await authService.getBlogByUserId(); // /api/blogs/me 호출
                commit('SET_USER_BLOG', blog);
                console.log('[Store Action] User blog fetched:', blog);
            } catch (error) {
                if (error.response && error.response.status === 404) {
                    console.log("사용자 블로그가 존재하지 않습니다.");
                    // 블로그가 없는 상태를 명확히 표시 (null 또는 특정 객체)
                    commit('SET_USER_BLOG', {notFound: true});
                } else {
                    console.error("[Store Action] 사용자 블로그 정보 가져오기 실패:", error);
                    commit('SET_USER_BLOG', null); // 실패 시 초기화
                }
            } finally {
                commit('SET_LOADING_BLOG', false);
            }
        },

        // 로그아웃 처리 액션
        async logoutAndClear({commit}) {
            console.log('[Store Action] Logging out and clearing data...');
            try {
                // authService의 로그아웃 호출 (백엔드 요청 및 로컬 스토리지 정리)
                await authService.logout(); // authService.logout 에서 clearAuthData 호출 가정
                // Vuex 상태 클리어
                commit('CLEAR_USER_DATA');
                console.log('[Store Action] Vuex state cleared.');
                // 로그아웃 후 리디렉션 등은 이 액션을 호출한 컴포넌트에서 처리
            } catch (error) {
                console.error('[Store Action] Logout failed:', error);
                // 에러가 발생해도 로컬 상태는 클리어 시도
                commit('CLEAR_USER_DATA');
                authService.clearAuthData(); // 확실하게 로컬 스토리지 정리
            }
        }
    },

    // 게터 (Getters): 상태를 기반으로 계산된 값을 반환 (Computed 속성과 유사)
    getters: {
        isLoggedIn: (state) => !!state.currentUser, // 로그인 여부
        username: (state) => state.currentUser?.username, // 사용자 이름
        nickName: (state) => state.currentUser?.nickName,
        userId: (state) => state.currentUser?.id, // 사용자 ID
        userProfileImageUrl: (state) => state.currentUser?.profileImageUrl, // 사용자 프로필 이미지 URL (필드명 확인)
        blogData: (state) => state.userBlog, // 블로그 전체 데이터
        blogUrl: (state) => state.userBlog?.url, // 블로그 URL
        hasBlog: (state) => state.userBlog && !state.userBlog.notFound, // 블로그 존재 여부 (notFound 플래그 사용 시)
        isLoading: (state) => state.isLoadingUser || state.isLoadingBlog, // 전체 로딩 상태 (간단 예시)
        blogId: (state) => state.userBlog?.id,      // 사용자 블로그 ID
        blogTitle: (state) => state.userBlog?.title,  // 사용자 블로그 제목
        isLoadingUser: (state) => state.isLoadingUser, // 이 줄이 있는지 확인하세요!
        isLoadingBlog: (state) => state.isLoadingBlog, // 이 줄이 있는지 확인하세요!
    }
});
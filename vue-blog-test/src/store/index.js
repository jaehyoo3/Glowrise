import {createStore} from 'vuex';
import authService from '@/services/authService'; // authService import

// --- advertisementService 임포트 ---
// authService 내부에 광고 함수를 추가했으므로 별도 임포트 불필요
// 만약 advertisementService.js 파일을 별도로 생성했다면 아래 주석 해제
// import advertisementService from '@/services/advertisementService';
// --- ---

export default createStore({
    // 상태 (State): 애플리케이션의 데이터
    state: {
        currentUser: authService.getStoredUser(), // 로컬 스토리지에서 초기 사용자 정보 로드
        userBlog: null,    // 현재 사용자의 블로그 정보
        isLoadingUser: false, // 사용자 정보 로딩 상태
        isLoadingBlog: false, // 블로그 정보 로딩 상태
        // --- 광고 목록 상태 추가 ---
        activeAdvertisements: [], // 활성 광고 목록
        // --- ---
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
                // 사용자 객체가 유효하면 상태 업데이트
                state.currentUser = {...user}; // 객체 복사하여 할당 (원본 불변성 유지)
            } else {
                // 사용자 정보가 없거나 유효하지 않으면 null로 설정
                state.currentUser = null;
            }
            // 최종 state.currentUser 값을 기반으로 로컬 스토리지 업데이트
            authService.setStoredUser(state.currentUser);
        },
        SET_USER_BLOG(state, blog) {
            state.userBlog = blog;
        },
        // 로그아웃 또는 초기화 시 사용자 관련 상태 클리어
        CLEAR_USER_DATA(state) {
            state.currentUser = null;
            state.userBlog = null;
        },
        // --- 광고 목록 업데이트 뮤테이션 추가 ---
        setActiveAdvertisements(state, advertisements) {
            // 항상 배열 형태를 유지하도록 || [] 추가
            state.activeAdvertisements = advertisements || [];
        },
        // --- ---
    },

    // 액션 (Actions): 비동기 작업 처리, 여러 뮤테이션 커밋 가능 (Dispatch로 호출)
    actions: {
        // 앱 로드 또는 로그인 후 현재 사용자 정보 가져오기
        async fetchCurrentUser({commit, state, dispatch}) {
            // 이미 로딩 중이면 중복 호출 방지
            if (state.isLoadingUser) return;
            commit('SET_LOADING_USER', true);
            try {
                console.log('[Store Action] Fetching current user...');
                const user = await authService.getCurrentUser(); // /api/users/me 호출
                commit('SET_CURRENT_USER', user); // 사용자 정보 뮤테이션 호출
                console.log('[Store Action] Current user fetched:', user);
                // 사용자 정보 로드 성공 시 & 블로그 정보 아직 없으면 블로그 정보 가져오기 시도
                if (user && !state.userBlog) {
                    await dispatch('fetchUserBlog'); // fetchUserBlog 액션 호출
                } else if (!user) {
                    // 사용자를 가져오지 못했다면 블로그 정보도 초기화
                    commit('SET_USER_BLOG', null);
                }
            } catch (error) {
                console.error("[Store Action] 현재 사용자 정보 가져오기 실패:", error);
                // 오류 발생 시 사용자 관련 모든 상태 초기화
                commit('CLEAR_USER_DATA');
                // 토큰 만료 등으로 인한 401 에러는 인터셉터에서 처리 후 재시도 될 수 있으나,
                // 최종적으로 실패하면 여기로 와서 상태가 초기화될 수 있음.
            } finally {
                commit('SET_LOADING_USER', false); // 로딩 상태 해제
            }
        },

        // 현재 사용자의 블로그 정보 가져오기
        async fetchUserBlog({commit, state}) {
            // 사용자 정보 없거나, 이미 블로그 정보 있거나, 로딩 중이면 중복 방지
            if (!state.currentUser || state.userBlog || state.isLoadingBlog) return;

            commit('SET_LOADING_BLOG', true);
            try {
                console.log('[Store Action] Fetching user blog...');
                const blog = await authService.getBlogByUserId(); // /api/blogs/me 호출
                commit('SET_USER_BLOG', blog); // 블로그 정보 뮤테이션 호출
                console.log('[Store Action] User blog fetched:', blog);
            } catch (error) {
                // 404 에러 (블로그 없음) 처리
                if (error.response && error.response.status === 404) {
                    console.log("사용자 블로그가 존재하지 않습니다.");
                    // 블로그가 없는 상태를 명확히 표시하기 위해 특정 객체 또는 null 설정
                    commit('SET_USER_BLOG', {notFound: true}); // 예시: notFound 플래그 사용
                } else {
                    // 기타 오류 처리
                    console.error("[Store Action] 사용자 블로그 정보 가져오기 실패:", error);
                    commit('SET_USER_BLOG', null); // 실패 시 null로 초기화
                }
            } finally {
                commit('SET_LOADING_BLOG', false); // 로딩 상태 해제
            }
        },

        // 로그아웃 처리 액션 (백엔드 API 호출 및 로컬 상태/스토리지 정리)
        async logoutAndClear({commit}) {
            console.log('[Store Action] Logging out and clearing data...');
            try {
                // authService의 logout 함수 호출 (내부적으로 백엔드 API 호출 및 로컬 스토리지 정리 수행)
                await authService.logout();
                // Vuex 상태 클리어
                commit('CLEAR_USER_DATA');
                console.log('[Store Action] Vuex state cleared.');
                // 리디렉션 등 후속 처리는 이 액션을 호출한 컴포넌트에서 담당
            } catch (error) {
                console.error('[Store Action] Logout failed:', error);
                // 오류 발생 시에도 Vuex 상태 및 로컬 스토리지 클리어 시도
                commit('CLEAR_USER_DATA');
                authService.clearAuthData(); // 확실하게 정리
            }
        },

        // --- 광고 목록 가져오기 액션 추가 ---
        async fetchActiveAdvertisements({commit}) {
            try {
                console.log('[Store Action] Fetching active advertisements...');
                // authService에 추가된 getActiveAdvertisements 함수 호출
                const advertisements = await authService.getActiveAdvertisements();
                // 뮤테이션 호출하여 스토어 상태 업데이트
                commit('setActiveAdvertisements', advertisements);
                console.log('[Store Action] Active advertisements fetched:', advertisements);
            } catch (error) {
                console.error("[Store Action] 활성 광고 목록 가져오기 실패:", error);
                commit('setActiveAdvertisements', []); // 실패 시 빈 배열로 설정
            }
        }
        // --- ---
    },

    // 게터 (Getters): 상태를 기반으로 계산된 값을 반환 (Computed 속성과 유사)
    getters: {
        isLoggedIn: (state) => !!state.currentUser, // currentUser 객체가 있으면 true
        username: (state) => state.currentUser?.username, // 옵셔널 체이닝 사용
        nickName: (state) => state.currentUser?.nickName,
        userId: (state) => state.currentUser?.id,
        userEmail: (state) => state.currentUser?.email, // 이메일 게터
        userProfileImage: (state) => state.currentUser?.profileImageUrl, // 실제 필드명 확인
        blogData: (state) => state.userBlog,
        blogUrl: (state) => state.userBlog?.url,
        hasBlog: (state) => state.userBlog && !state.userBlog.notFound, // notFound 플래그 확인
        isLoading: (state) => state.isLoadingUser || state.isLoadingBlog, // 사용자 또는 블로그 로딩 중이면 true
        blogId: (state) => state.userBlog?.id,
        blogTitle: (state) => state.userBlog?.title,
        isLoadingUser: (state) => state.isLoadingUser,
        isLoadingBlog: (state) => state.isLoadingBlog,

        // --- 관리자 여부 확인 게터 추가 ---
        isAdmin: (state) => {
            // currentUser 상태와 role 필드가 있는지 확인하고 값 비교
            // 실제 백엔드에서 사용하는 역할 이름 ('ADMIN', 'ROLE_ADMIN' 등)으로 수정 필요
            return state.currentUser && state.currentUser.role === 'ROLE_ADMIN';
        },
        // --- ---

        // --- 활성 광고 목록 게터 추가 ---
        activeAdvertisements: state => state.activeAdvertisements,
        // --- ---
    }
});
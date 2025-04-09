// src/services/authService.js
import axios from 'axios';

// API 및 프론트엔드 URL (환경 변수 등으로 관리하는 것이 좋습니다)
const API_URL = 'http://localhost:8080';

// Axios 인스턴스 생성
const apiClient = axios.create({
    baseURL: API_URL,
    withCredentials: true, // 쿠키 등 자격 증명 정보 포함 요청
});

// --- 로컬 스토리지 Helper Functions ---
const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    // 리프레시 토큰은 로그인 또는 토큰 갱신 시에만 받을 수 있으므로, 있을 때만 저장
    if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
    }
};

const getStoredUser = () => {
    try {
        const userString = localStorage.getItem('user');
        return userString ? JSON.parse(userString) : null;
    } catch (error) {
        console.error("저장된 사용자 정보 파싱 실패:", error);
        localStorage.removeItem('user'); // 잘못된 데이터 제거
        return null;
    }
};

const setStoredUser = (user) => {
    // 유효한 사용자 객체(id 또는 username 등 식별자 포함)일 경우 저장
    if (user && (user.id || user.username)) {
        localStorage.setItem('user', JSON.stringify(user));
    } else {
        // null 또는 유효하지 않은 user 객체가 들어오면 로컬 스토리지에서 제거
        localStorage.removeItem('user');
    }
};

// 로컬 스토리지의 인증 관련 데이터 모두 삭제
const clearAuthData = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    console.log('[AuthService] Local storage (tokens, user) cleared.');
    // Vuex 상태는 여기서 직접 지우지 않음 (Store Action에서 처리)
};

// 로그아웃 처리 시 리디렉션 등 추가 로직 (필요한 경우)
const handleLogoutRedirect = () => {
    // 로그아웃 후 로그인 페이지 등으로 이동
    if (typeof window !== 'undefined') {
        // window.location.href = `${FRONTEND_URL}/login`; // 로그인 페이지 경로로 수정
        // 또는 router 인스턴스를 가져와서 프로그래매틱 네비게이션 사용
    }
}

// --- Axios Interceptors ---
// 요청 인터셉터: 모든 요청에 자동으로 토큰 추가 (토큰 갱신 요청 제외)
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        // 토큰이 있고, 토큰 갱신 요청이 아닐 때만 헤더 추가
        if (token && !config.url.endsWith('/api/users/refresh')) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러 시 토큰 자동 갱신 시도
apiClient.interceptors.response.use(
    (response) => response, // 성공 응답은 그대로 통과
    async (error) => {
        const originalRequest = error.config;
        // 401 에러, 재시도 아님, 토큰 갱신 요청 아님 조건 확인
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.endsWith('/api/users/refresh')) {
            originalRequest._retry = true; // 재시도 플래그 설정 (무한 루프 방지)
            const currentRefreshToken = getRefreshToken();

            if (!currentRefreshToken) {
                console.error("401 Unauthorized - 리프레시 토큰 없음. 로그아웃 처리 필요.");
                // Vuex 스토어 액션 호출 또는 직접 로그아웃 처리
                clearAuthData(); // 최소한 로컬 데이터 정리
                handleLogoutRedirect(); // 로그인 페이지 이동 등
                return Promise.reject(error);
            }

            try {
                console.info("액세스 토큰 만료. 토큰 갱신 시도...");
                // 토큰 갱신 API 직접 호출 (authService 객체 생성 전/후 문제 피하기 위해 직접 호출)
                const refreshResponse = await apiClient.post('/api/users/refresh', {refreshToken: currentRefreshToken});
                const {accessToken} = refreshResponse.data;

                setTokens(accessToken); // 새 액세스 토큰 저장 (리프레시 토큰은 건드리지 않음)
                console.info("토큰 갱신 성공.");
                console.log("새 Access Token:", accessToken); // <-- 이 로그 추가

                // 원래 요청 헤더에 새 토큰 설정 후 재시도
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
                return apiClient(originalRequest);

            } catch (refreshError) {
                console.error("토큰 갱신 실패:", refreshError);
                // 리프레시 토큰마저 만료/무효화된 경우 로그아웃 처리
                clearAuthData();
                handleLogoutRedirect(); // 로그인 페이지 이동 등
                // Vuex 스토어에도 알릴 필요 있음 (예: 이벤트를 발생시키거나 스토어 액션 직접 호출)
                // store.dispatch('logoutAndClear'); // main.js 등에서 스토어 인스턴스 접근 가능 시
                return Promise.reject(refreshError);
            }
        }
        // 401 에러가 아니거나 다른 조건 불충족 시 에러 그대로 반환
        return Promise.reject(error);
    }
);


// --- AuthService 객체 ---
// 각 API 엔드포인트 호출을 담당하는 함수 모음
const authService = {
    // --- 인증 관련 ---
    login: async (email, password) => {
        const response = await apiClient.post(
            '/login',
            new URLSearchParams({email, password}), // form-urlencoded 형식
            {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
        );
        // 로그인 성공 시 받은 정보 (토큰, 사용자 기본 정보 등)
        const {accessToken, refreshToken, username, userId} = response.data; // 백엔드 응답 필드 확인
        const user = {id: userId, username}; // 로컬 저장용 사용자 객체
        setTokens(accessToken, refreshToken); // 토큰 저장
        setStoredUser(user); // 사용자 정보 로컬 저장 (Vuex와 별개로 빠른 접근용)
        // Vuex 상태 업데이트는 이 함수 호출 후, 스토어 액션에서 담당
        return response.data; // 전체 응답 데이터 반환
    },

    signup: async (userData) => {
        const response = await apiClient.post('/api/users/signup', userData);
        return response.data;
    },

    // 로그아웃: 백엔드 호출 및 로컬 스토리지 정리
    logout: async () => {
        try {
            await apiClient.get('/api/users/logout'); // 백엔드 로그아웃 호출 (선택적)
        } catch (error) {
            console.error("백엔드 로그아웃 API 호출 실패:", error);
        } finally {
            // 항상 로컬 인증 정보 클리어
            clearAuthData();
            console.log("[AuthService] Logout: Local data cleared.");
            // Vuex 상태 클리어는 이 함수를 호출하는 스토어 액션('logoutAndClear')에서 담당
        }
        // return { message: "로그아웃 처리 완료" }; // 필요시 반환
    },

    // 현재 사용자 정보 가져오기 (API 호출만)
    getCurrentUser: async () => {
        try {
            const response = await apiClient.get('/api/users/me');
            return response.data; // 성공 시 사용자 데이터 반환
        } catch (error) {
            // 에러 발생 시 그대로 throw하여 호출 측(Vuex 액션)에서 처리하도록 함
            console.error("[AuthService] getCurrentUser 실패:", error.response?.status, error.message);
            throw error;
        }
    },

    // 사용자 프로필 업데이트
    updateUserProfile: async (username, userData) => {
        // PUT 요청 시 username 파라미터 사용 확인 (API 경로 확인)
        const response = await apiClient.put(`/api/users/profile/${username}`, userData);
        // 성공 시 Vuex 상태 업데이트는 별도 액션 또는 getCurrentUser 재호출 고려
        return response.data;
    },

    // 사용자 닉네임 업데이트 (API 엔드포인트 확인 필요)
    updateUserNickname: async (nickname) => {
        // API 엔드포인트 및 요청 본문 형식 확인 필요
        const response = await apiClient.put('/api/users/me/nickname', {nickName: nickname}); // 예시
        // 성공 시 Vuex 상태 업데이트 필요
        return response.data;
    },

    // 토큰 갱신 (인터셉터에서 직접 apiClient 사용 권장하나, 필요시 외부에 노출)
    // refreshToken: async (refreshTokenValue) => { ... },

    // --- 블로그 관련 ---
    getBlogById: async (blogId) => {
        const response = await apiClient.get(`/api/blogs/id/${blogId}`);
        return response.data;
    },

    // 현재 사용자의 블로그 정보 가져오기 (API 호출만)
    getBlogByUserId: async () => {
        try {
            const response = await apiClient.get('/api/blogs/me');
            return response.data; // 성공 시 블로그 데이터 반환
        } catch (error) {
            // 에러 발생 시 그대로 throw하여 호출 측(Vuex 액션)에서 처리하도록 함
            console.error("[AuthService] getBlogByUserId 실패:", error.response?.status, error.message);
            throw error;
        }
    },

    getBlogByUrl: async (url) => {
        const response = await apiClient.get(`/api/blogs/${url}`);
        return response.data;
    },

    checkBlogUrlAvailability: async (url) => {
        // Debounce/Throttle은 UI 컴포넌트 레벨에서 적용
        const response = await apiClient.get(`/api/blogs/check-url?url=${encodeURIComponent(url)}`);
        return response.data.available; // 백엔드 응답 형식 가정 ({ available: boolean })
    },

    createBlog: async (blogData) => {
        const response = await apiClient.post('/api/blogs', blogData);
        // 성공 시 Vuex 상태 업데이트 또는 관련 데이터 재조회 필요
        return response.data;
    },

    updateBlog: async (blogId, blogData) => {
        const response = await apiClient.put(`/api/blogs/${blogId}`, blogData);
        // 성공 시 Vuex 상태 업데이트 또는 관련 데이터 재조회 필요
        return response.data;
    },

    deleteBlog: async (blogId, userId) => {
        // userId 파라미터 사용 확인
        await apiClient.delete(`/api/blogs/${blogId}`, {params: {userId}});
        // 성공 시 Vuex 상태 업데이트 또는 관련 데이터 재조회 필요
    },

    // --- 메뉴 관련 ---
    getMenusByBlogId: async (blogId) => {
        const response = await apiClient.get(`/api/menus/blog/${blogId}`);
        return response.data;
    },

    createMenu: async (menuData) => {
        const response = await apiClient.post('/api/menus', menuData, {
            headers: {'Content-Type': 'application/json'},
        });
        // 성공 시 관련 메뉴 목록 재조회 등 필요
        return response.data;
    },

    updateMenuOrder: async (blogId, menus) => {
        const response = await apiClient.put(`/api/menus/${blogId}/order`, menus, {
            headers: {'Content-Type': 'application/json'},
        });
        // 성공 시 관련 메뉴 목록 재조회 등 필요
        return response.data;
    },

    // --- 게시글 관련 ---
    createPost: async (postData, files) => {
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files) {
            files.forEach((file) => formData.append('files', file));
        }
        const response = await apiClient.post('/api/posts', formData, {
            headers: {'Content-Type': 'multipart/form-data'},
        });
        // 성공 시 관련 글 목록 재조회 등 필요
        return response.data;
    },

    updatePost: async (postId, postData, files) => {
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files && files.length > 0) {
            files.forEach((file) => formData.append('files', file));
        }
        const response = await apiClient.put(`/api/posts/${postId}`, formData, {
            headers: {'Content-Type': 'multipart/form-data'},
        });
        // 성공 시 해당 글 상세 정보 또는 관련 목록 재조회 필요
        return response.data;
    },

    deletePost: async (postId, userId) => {
        // userId 파라미터 사용 확인
        const response = await apiClient.delete(`/api/posts/${postId}`, {params: {userId}});
        // 성공 시 관련 글 목록 재조회 등 필요
        return response.data; // 백엔드 응답 형식 확인
    },

    getPostById: async (postId) => {
        const response = await apiClient.get(`/api/posts/${postId}`);
        return response.data;
    },

    getPostsByBlogIdAndMenuId: async (blogId, menuId, params = {}) => {
        const url = menuId ? `/api/posts/blog/${blogId}/${menuId}` : `/api/posts/blog/${blogId}`;
        const response = await apiClient.get(url, {
            params: {
                searchKeyword: params.searchKeyword,
                page: params.page || 0,
                size: params.size || 10, // 페이지 크기 조정 고려
                sort: params.sort || 'createdAt,desc', // API 기본 정렬 확인
            },
        });
        return response.data; // 페이지 정보 포함된 객체 반환 예상
    },

    getPopularPosts: async (period = 'WEEKLY') => {
        const response = await apiClient.get('/api/posts/getPopular', {params: {period}});
        return response.data;
    },

    // --- 댓글 관련 ---
    getCommentsByPostId: async (postId) => {
        const response = await apiClient.get(`/api/comments/post/${postId}`);
        return response.data;
    },
    createComment: async (commentData) => {
        const response = await apiClient.post('/api/comments', commentData);
        // 성공 시 댓글 목록 재조회 필요
        return response.data;
    },
    createReply: async (parentId, replyData) => {
        const response = await apiClient.post(`/api/comments/${parentId}/reply`, replyData);
        // 성공 시 댓글 목록 재조회 필요
        return response.data;
    },

    // --- 파일 관련 ---
    getFileById: async (fileId) => {
        const response = await apiClient.get(`/api/files/${fileId}`);
        return response.data;
    },
    getFileDownloadUrl: (fileId) => {
        return `${API_URL}/api/files/download/${fileId}`;
    },
    uploadInlineImage: async (formData) => {
        const response = await apiClient.post('/api/files/upload/inline', formData, {
            headers: {'Content-Type': 'multipart/form-data'}
        });
        // { imageUrl: '...' } 형태의 응답 기대
        return response.data;
    },

    // --- 알림 관련 ---
    getNotifications: async () => {
        const response = await apiClient.get('/api/notifications');
        return response.data;
    },
    markNotificationAsRead: async (notificationId) => {
        // PUT 요청 본문 필요 여부 확인 (비어있으면 {} 전달)
        const response = await apiClient.put(`/api/notifications/${notificationId}/read`, {});
        // 성공 시 알림 목록 상태 업데이트 필요
        return response.data;
    },

    // --- 로컬 스토리지 Helper 함수 외부 노출 ---
    // (Vuex 스토어 외부에서 로컬 스토리지 접근 필요 시 사용)
    getStoredUser: getStoredUser,
    setStoredUser: setStoredUser,
    clearAuthData: clearAuthData,
    getAccessToken: getAccessToken,
};

export default authService;
import axios from 'axios';

// API 주소는 요청대로 변경하지 않습니다.
const API_URL = 'http://localhost:8080';
const FRONTEND_URL = 'http://localhost:3000';

// Axios 인스턴스 생성 및 기본 설정
const apiClient = axios.create({
    baseURL: API_URL, // API 요청 시 baseURL을 사용하여 경로를 간결하게 작성
    withCredentials: true, // 자격 증명(쿠키 등)을 요청에 포함시킴
});

// --- 헬퍼 함수 ---
// 로컬 스토리지에서 토큰을 가져오는 함수
const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

// 로컬 스토리지에 토큰을 저장하는 함수
const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    // refreshToken은 로그인 시 등에만 제공되므로, 있을 경우에만 저장
    if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
    }
};

// 로컬 스토리지에서 사용자 정보를 가져오는 함수 (JSON 파싱 오류 처리 포함)
const getStoredUser = () => {
    try {
        const userString = localStorage.getItem('user');
        // userString이 존재하면 JSON으로 파싱, 아니면 null 반환
        return userString ? JSON.parse(userString) : null;
    } catch (error) {
        console.error("저장된 사용자 정보 파싱 실패:", error);
        localStorage.removeItem('user'); // 잘못된 데이터는 제거
        return null;
    }
};

// 로컬 스토리지에 사용자 정보를 저장하는 함수
const setStoredUser = (user) => {
    // 유효한 사용자 객체 (id 또는 username 속성 존재)만 저장
    if (user && (user.id || user.username)) {
        localStorage.setItem('user', JSON.stringify(user));
    } else {
        localStorage.removeItem('user'); // 유효하지 않으면 제거
    }
};

// 로컬 스토리지에서 인증 관련 데이터를 모두 지우는 함수
const clearAuthData = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    // apiClient의 기본 헤더를 지울 필요는 없음. 요청 인터셉터가 매번 처리하기 때문.
};

// 로그아웃 처리 로직 (데이터 클리어 및 리디렉션)
const handleLogout = () => {
    clearAuthData();
    // 브라우저 환경에서만 실행되도록 확인 후 로그인 페이지로 리디렉션
    if (typeof window !== 'undefined') {
        window.location.href = `${FRONTEND_URL}/login`;
    }
};


// --- Axios 인터셉터 ---

// 요청 인터셉터: 모든 요청 전에 실행됨
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        // 토큰이 존재하고, 토큰 갱신 요청(/api/users/refresh)이 아닐 경우 Authorization 헤더 추가
        // (토큰 갱신 요청 자체에는 만료된 토큰을 보내지 않거나, 백엔드 정책에 따라 다름)
        if (token && !config.url.endsWith('/api/users/refresh')) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        // 토큰이 없으면 헤더 없이 요청 (공개 API 접근 등)
        return config;
    },
    (error) => Promise.reject(error) // 요청 설정 중 오류 발생 시 처리
);

// 응답 인터셉터: 모든 응답 수신 시 실행됨
apiClient.interceptors.response.use(
    (response) => response, // 성공적인 응답은 그대로 통과
    async (error) => {
        const originalRequest = error.config; // 실패한 원래 요청 정보

        // 401 Unauthorized 에러이고, 재시도된 요청이 아니며, 토큰 갱신 요청 자체가 아닐 경우
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.endsWith('/api/users/refresh')) {
            originalRequest._retry = true; // 재시도 플래그 설정 (무한 루프 방지)
            const currentRefreshToken = getRefreshToken(); // 리프레시 토큰 가져오기

            // 리프레시 토큰이 없으면 갱신 불가, 로그아웃 처리
            if (!currentRefreshToken) {
                console.error("401 Unauthorized - 리프레시 토큰 없음. 로그아웃 처리.");
                handleLogout();
                return Promise.reject(error); // 원래 에러 반환
            }

            try {
                console.info("액세스 토큰 만료. 토큰 갱신 시도...");
                // authService의 refreshToken 함수를 직접 호출 (동일한 apiClient 인스턴스 사용)
                const response = await authService.refreshToken(currentRefreshToken); // 토큰 명시적 전달
                const {accessToken} = response.data; // 새 액세스 토큰

                // 로컬 스토리지에 새 액세스 토큰 저장
                setTokens(accessToken);

                // 실패했던 원래 요청의 헤더를 새 토큰으로 업데이트하여 재요청 준비
                // 앞으로의 요청은 요청 인터셉터가 처리하므로 기본 헤더 설정 불필요
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;

                console.info("토큰 갱신 성공. 원래 요청 재시도.");
                // 원래 요청을 새 토큰으로 재시도
                return apiClient(originalRequest);

            } catch (refreshError) {
                console.error("토큰 갱신 실패:", refreshError);
                // 리프레시 토큰마저 만료/무효화된 경우 등 갱신 실패 시 로그아웃 처리
                handleLogout();
                return Promise.reject(refreshError); // 갱신 에러 반환
            }
        }

        // 401 에러가 아니거나 다른 종류의 에러는 그대로 반환
        return Promise.reject(error);
    }
);


// --- AuthService 객체 ---
// 각 API 요청 함수들을 정의
const authService = {
    login: async (email, password) => {
        // try/catch 불필요: 에러는 자연스럽게 전파되거나 인터셉터에서 처리됨
        const response = await apiClient.post(
            '/login', // baseURL 기준으로 상대 경로 사용
            new URLSearchParams({email, password}), // x-www-form-urlencoded 형식 데이터
            {headers: {'Content-Type': 'application/x-www-form-urlencoded'}} // Content-Type 명시
        );
        const {accessToken, refreshToken, username, userId} = response.data;
        const user = {id: userId, username};

        // 토큰과 사용자 정보 저장
        setTokens(accessToken, refreshToken);
        setStoredUser(user);

        // 기본 헤더 설정 불필요 (요청 인터셉터가 담당)
        return response.data;
    },

    signup: async (userData) => {
        const response = await apiClient.post('/api/users/signup', userData);
        return response.data;
    },

    logout: async () => {
        // 백엔드 로그아웃 API 호출 시도 (선택 사항)
        try {
            // 로컬 데이터를 먼저 지우더라도 API 호출 시도 가능
            await apiClient.get('/api/users/logout');
        } catch (error) {
            // 백엔드 호출 실패해도 로컬 로그아웃은 진행
            console.error("백엔드 로그아웃 실패, 클라이언트 측 로그아웃 진행:", error);
        } finally {
            // 항상 로컬 데이터 클리어
            clearAuthData();
            // 인터셉터의 리디렉션 외에 추가적인 상태 업데이트나 리디렉션 로직 필요 시 여기에 추가
            console.log("로컬에서 로그아웃 처리됨.");
        }
        // 필요에 따라 성공 메시지 등 반환
        return {message: "로그아웃 성공"};
    },

    getCurrentUser: async () => {
        // 토큰 확인 로직 불필요 (인터셉터가 헤더 추가/미추가 담당)
        // 토큰이 없으면 인증되지 않은 요청으로 백엔드가 접근 제어
        // 토큰이 유효하지 않으면 인터셉터가 갱신 또는 로그아웃 처리
        try {
            const response = await apiClient.get('/api/users/me');
            return response.data;
        } catch (error) {
            // 재시도 후에도 실패한 경우 (권한 없음, 서버 에러 등)
            console.error("현재 사용자 정보 가져오기 실패:", error.response?.data || error.message);
            return null; // 사용자 정보 가져오기 실패 시 null 반환
        }
    },

    updateUserProfile: async (username, userData) => {
        // 인증 필요 - 인터셉터가 토큰 처리
        const response = await apiClient.put(`/api/users/profile/${username}`, userData);
        return response.data;
    },

    // **새로 추가된 닉네임 변경 함수**
    updateUserNickname: async (nickname) => {
        // 인증 필요 - 인터셉터가 토큰 자동 추가
        // 요청 본문의 키를 "nickname" 대신 "nickName" (N 대문자)으로 변경
        await apiClient.put('/api/users/me/nickname', {nickName: nickname});
        // 성공 시 로컬 스토리지의 사용자 정보도 업데이트 (선택 사항)
        const user = authService.getStoredUser();
        if (user) {
            user.nickName = nickname;
            authService.setStoredUser(user);
        }
    },

    // 토큰 갱신 함수 (인터셉터 및 외부에서 사용 가능)
    // refreshTokenValue를 인자로 받아 localStorage 의존성 줄임 (인터셉터 로직 내 복잡성 방지)
    refreshToken: async (refreshTokenValue) => {
        if (!refreshTokenValue) throw new Error('리프레시 토큰 없음');
        // 이 요청은 401 인터셉터의 갱신 로직을 다시 트리거하면 안됨
        // (인터셉터에서 이미 `!originalRequest.url.endsWith('/api/users/refresh')` 확인)
        const response = await apiClient.post(
            '/api/users/refresh',
            {refreshToken: refreshTokenValue}
            // 갱신 요청에는 보통 Authorization 헤더 불필요
            // `withCredentials: true`는 apiClient 기본값으로 설정되어 있음
        );
        // 성공 시 새 액세스 토큰 저장 (로그인과 달리 리프레시 토큰은 재발급되지 않을 수 있음)
        const {accessToken} = response.data;
        localStorage.setItem('accessToken', accessToken); // setTokens 사용 대신 직접 설정 (refreshToken은 건드리지 않음)
        return response; // 인터셉터 등에서 사용할 수 있도록 전체 응답 반환
    },

    // --- 블로그 관련 함수 ---
    getBlogById: async (blogId) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/blogs/id/${blogId}`);
        return response.data;
    },

    getBlogByUserId: async () => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.get('/api/blogs/me');
        return response.data;
    },

    getBlogByUrl: async (url) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/blogs/${url}`);
        return response.data;
    },

    checkBlogUrlAvailability: async (url) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.get(`/api/blogs/check-url?url=${encodeURIComponent(url)}`);
        return response.data.available; // API 응답 구조가 { available: boolean } 이라고 가정
    },

    createBlog: async (blogData) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.post('/api/blogs', blogData);
        return response.data;
    },

    updateBlog: async (blogId, blogData) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.put(`/api/blogs/${blogId}`, blogData);
        return response.data;
    },

    deleteBlog: async (blogId, userId) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        // 원본 코드처럼 userId를 쿼리 파라미터로 전달
        await apiClient.delete(`/api/blogs/${blogId}`, {params: {userId}});
        // 삭제 요청은 보통 반환값이 없거나, 성공 상태/메시지만 반환
    },

    // --- 메뉴 관련 함수 ---
    getMenusByBlogId: async (blogId) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/menus/blog/${blogId}`);
        return response.data;
    },

    createMenu: async (menuData) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.post('/api/menus', menuData, {
            headers: {'Content-Type': 'application/json'}, // JSON 데이터 전송 시 Content-Type 명시
        });
        return response.data;
    },

    updateMenuOrder: async (blogId, menus) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.put(`/api/menus/${blogId}/order`, menus, {
            headers: {'Content-Type': 'application/json'}, // JSON 데이터 전송 시 Content-Type 명시
        });
        return response.data;
    },

    // --- 게시글 관련 함수 ---
    createPost: async (postData, files) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const formData = new FormData();
        // JSON 데이터를 Blob으로 변환하여 FormData에 추가 (파일과 함께 전송)
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files) {
            files.forEach((file) => formData.append('files', file)); // 파일 추가
        }
        const response = await apiClient.post('/api/posts', formData, {
            // Content-Type은 FormData 사용 시 Axios가 자동으로 multipart/form-data로 설정하는 경우가 많지만, 명시해도 무방
            headers: {'Content-Type': 'multipart/form-data'},
        });
        return response.data;
    },

    updatePost: async (postId, postData, files) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files && files.length > 0) {
            files.forEach((file) => formData.append('files', file));
        }
        const response = await apiClient.put(`/api/posts/${postId}`, formData, {
            headers: {'Content-Type': 'multipart/form-data'},
        });
        return response.data;
    },

    deletePost: async (postId, userId) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        // 원본 코드처럼 userId를 쿼리 파라미터로 전달
        const response = await apiClient.delete(`/api/posts/${postId}`, {params: {userId}});
        return response.data;
    },

    getPostById: async (postId) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/posts/${postId}`);
        return response.data;
    },

    getPostsByBlogIdAndMenuId: async (blogId, menuId, params = {}) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const url = menuId ? `/api/posts/blog/${blogId}/${menuId}` : `/api/posts/blog/${blogId}`;
        const response = await apiClient.get(url, {
            params: { // 요청 파라미터 설정
                searchKeyword: params.searchKeyword,
                page: params.page || 0,
                size: params.size || 20,
                sort: params.sort || 'updatedAt,desc', // 기본 정렬값
            },
        });
        return response.data;
    },

    getPopularPosts: async (period = 'WEEKLY') => {
        // 공개 접근 - 토큰 불필요
        const response = await apiClient.get('/api/posts/getPopular', {params: {period}});
        return response.data;
    },

    // --- 댓글 관련 함수 ---
    getCommentsByPostId: async (postId) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/comments/post/${postId}`);
        return response.data;
    },

    createComment: async (commentData) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.post('/api/comments', commentData);
        return response.data;
    },

    createReply: async (parentId, replyData) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.post(`/api/comments/${parentId}/reply`, replyData);
        return response.data;
    },

    // --- 파일 관련 함수 ---
    getFileById: async (fileId) => {
        // 공개 또는 인증 접근 - 인터셉터가 토큰 있으면 추가
        const response = await apiClient.get(`/api/files/${fileId}`);
        return response.data;
    },

    getFileDownloadUrl: (fileId) => {
        // API 호출 없이 다운로드 URL만 생성하여 반환
        return `${API_URL}/api/files/download/${fileId}`;
    },

    // --- 알림 관련 함수 ---
    getNotifications: async () => {
        // 인증 필요 - 인터셉터가 토큰 추가
        const response = await apiClient.get('/api/notifications');
        return response.data;
    },

    markNotificationAsRead: async (notificationId) => {
        // 인증 필요 - 인터셉터가 토큰 추가
        // PUT 요청 시 본문이 비어있으면 빈 객체 {} 전달
        const response = await apiClient.put(`/api/notifications/${notificationId}/read`, {});
        return response.data;
    },

    // 로컬 스토리지 헬퍼 함수들 (외부에서 필요 시 사용하도록 노출)
    getStoredUser: getStoredUser,
    setStoredUser: setStoredUser,
};

export default authService;
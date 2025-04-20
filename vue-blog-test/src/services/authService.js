import axios from 'axios';

// API 서버 주소 (실제 환경에 맞게 수정 필요)
const API_URL = 'http://localhost:8080';

// Axios 인스턴스 생성 (baseURL, 자격증명 포함 옵션 설정)
const apiClient = axios.create({
    baseURL: API_URL,
    withCredentials: true, // 쿠키, 인증 헤더 등을 자동으로 주고받도록 설정
});

// 로컬 스토리지에서 토큰 가져오는 함수
const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

// 로컬 스토리지에 토큰 저장하는 함수
const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) { // 리프레시 토큰이 있을 경우에만 저장
        localStorage.setItem('refreshToken', refreshToken);
    }
};

// 로컬 스토리지에서 사용자 정보 가져오는 함수 (JSON 파싱 오류 처리 포함)
const getStoredUser = () => {
    try {
        const userString = localStorage.getItem('user');
        return userString ? JSON.parse(userString) : null;
    } catch (error) {
        console.error("로컬 스토리지 사용자 정보 파싱 오류:", error);
        localStorage.removeItem('user'); // 오류 발생 시 잘못된 데이터 제거
        return null;
    }
};

// 로컬 스토리지에 사용자 정보 저장하는 함수 (유효한 사용자 객체인지 확인)
const setStoredUser = (user) => {
    if (user && (user.id || user.username)) { // id 또는 username이 있는 유효한 객체일 경우
        localStorage.setItem('user', JSON.stringify(user));
    } else {
        localStorage.removeItem('user'); // 유효하지 않거나 null이면 제거
    }
};

// 로컬 스토리지의 모든 인증 관련 데이터 제거 함수
const clearAuthData = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
};

// 로그아웃 시 리디렉션 처리 함수 (현재 주석 처리됨)
const handleLogoutRedirect = () => {
    if (typeof window !== 'undefined') {
        // 필요하다면 로그인 페이지 등으로 리디렉션
        // window.location.href = '/login';
        console.log("로그아웃 감지, 리디렉션 필요 시 여기에 구현");
    }
}

// Axios 요청 인터셉터: 모든 요청 보내기 전에 실행
apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken(); // 로컬 스토리지에서 액세스 토큰 가져오기
        // 토큰이 있고, 리프레시 요청이 아닐 경우 Authorization 헤더 추가
        if (token && !config.url.endsWith('/api/users/refresh')) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config; // 수정된 설정 반환
    },
    (error) => Promise.reject(error) // 요청 오류 발생 시 처리
);

// Axios 응답 인터셉터: 모든 응답 받기 전에 실행
apiClient.interceptors.response.use(
    (response) => response, // 성공적인 응답은 그대로 반환
    async (error) => { // 오류 응답 처리
        const originalRequest = error.config; // 원래 요청 정보 저장

        // 401 Unauthorized 에러이고, 재시도한 요청이 아니며, 리프레시 요청이 아닐 경우
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.endsWith('/api/users/refresh')) {
            originalRequest._retry = true; // 재시도 플래그 설정 (무한 재시도 방지)
            const currentRefreshToken = getRefreshToken(); // 로컬 스토리지에서 리프레시 토큰 가져오기

            // 리프레시 토큰이 없으면 인증 정보 클리어 및 로그아웃 처리
            if (!currentRefreshToken) {
                console.log("리프레시 토큰 없음, 로그아웃 처리.");
                clearAuthData();
                handleLogoutRedirect();
                return Promise.reject(error); // 원래 에러 반환
            }

            try {
                // 리프레시 토큰으로 새 액세스 토큰 발급 요청 (POST /api/users/refresh)
                console.log("액세스 토큰 갱신 시도...");
                const refreshResponse = await apiClient.post('/api/users/refresh', {refreshToken: currentRefreshToken});
                const {accessToken} = refreshResponse.data; // 새 액세스 토큰 추출

                setTokens(accessToken); // 새 액세스 토큰 로컬 스토리지에 저장
                console.log("새 액세스 토큰 발급 성공.");

                // 원래 요청의 Authorization 헤더를 새 토큰으로 교체
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
                // 원래 요청 재시도
                return apiClient(originalRequest);

            } catch (refreshError) {
                // 리프레시 토큰 발급 실패 시 (리프레시 토큰 만료 등)
                console.error("토큰 갱신 실패:", refreshError);
                clearAuthData(); // 모든 인증 정보 클리어
                handleLogoutRedirect(); // 로그아웃 처리
                return Promise.reject(refreshError); // 갱신 에러 반환
            }
        }
        // 401 에러가 아니거나 재시도 조건 미충족 시 원래 에러 반환
        return Promise.reject(error);
    }
);

// authService 객체 정의 (API 호출 함수 모음)
const authService = {
    // 로그인 API 호출
    login: async (email, password) => {
        // form-urlencoded 형식으로 데이터 전송
        const response = await apiClient.post(
            '/login',
            new URLSearchParams({email, password}),
            {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
        );
        // 응답 데이터에서 토큰 및 사용자 정보 추출
        const {accessToken, refreshToken, username, userId, nickName, role} = response.data; // nickName, role 추가 (백엔드 응답 확인 필요)
        // 사용자 객체 생성 (스토어 및 로컬 스토리지 저장용)
        const user = {id: userId, username: username, nickName: nickName, role: role}; // nickName, role 추가
        setTokens(accessToken, refreshToken); // 토큰 저장
        setStoredUser(user); // 사용자 정보 저장
        return response.data; // 전체 응답 데이터 반환
    },

    // 회원가입 API 호출
    signup: async (userData) => {
        const response = await apiClient.post('/api/users/signup', userData);
        return response.data;
    },

    // 로그아웃 API 호출 및 로컬 데이터 클리어
    logout: async () => {
        try {
            // 백엔드 로그아웃 API 호출 (선택적)
            await apiClient.get('/api/users/logout');
        } catch (error) {
            // 로그아웃 API 호출 실패는 일반적으로 무시해도 괜찮음 (클라이언트 데이터는 정리)
            console.warn("백엔드 로그아웃 API 호출 실패 (무시 가능):", error);
        } finally {
            // 로컬 스토리지의 인증 정보 무조건 클리어
            clearAuthData();
        }
    },

    // 현재 로그인한 사용자 정보 가져오기 API 호출 (try/catch 제거됨)
    getCurrentUser: async () => {
        const response = await apiClient.get('/api/users/me'); // GET /api/users/me
        return response.data; // 사용자 정보 객체 반환
    },

    // 사용자 프로필 업데이트 API 호출
    updateUserProfile: async (username, userData) => {
        const response = await apiClient.put(`/api/users/profile/${username}`, userData);
        return response.data;
    },

    // 사용자 닉네임 업데이트 API 호출
    updateUserNickname: async (nickname) => {
        const response = await apiClient.put('/api/users/me/nickname', {nickName: nickname});
        return response.data;
    },

    // --- 블로그 관련 API ---
    getBlogById: async (blogId) => {
        const response = await apiClient.get(`/api/blogs/id/${blogId}`);
        return response.data;
    },
    // 사용자 ID로 블로그 정보 가져오기 API 호출 (try/catch 제거됨)
    getBlogByUserId: async () => {
        const response = await apiClient.get('/api/blogs/me');
        return response.data;
    },
    getBlogByUrl: async (url) => {
        const response = await apiClient.get(`/api/blogs/${url}`);
        return response.data;
    },
    checkBlogUrlAvailability: async (url) => {
        const response = await apiClient.get(`/api/blogs/check-url?url=${encodeURIComponent(url)}`);
        return response.data;
    },
    createBlog: async (blogData) => {
        const response = await apiClient.post('/api/blogs', blogData);
        return response.data;
    },
    updateBlog: async (blogId, blogData) => {
        const response = await apiClient.patch(`/api/blogs/${blogId}`, blogData);
        return response.data;
    },
    deleteBlog: async (blogId) => {
        await apiClient.delete(`/api/blogs/${blogId}`);
    },

    // --- 메뉴 관련 API ---
    getMenusByBlogId: async (blogId) => {
        const response = await apiClient.get(`/api/menus/blog/${blogId}`);
        return response.data;
    },
    createMenu: async (menuData) => {
        const response = await apiClient.post('/api/menus', menuData, {
            headers: {'Content-Type': 'application/json'},
        });
        return response.data;
    },
    updateMenuOrder: async (blogId, menus) => {
        const response = await apiClient.put(`/api/menus/${blogId}/order`, menus, {
            headers: {'Content-Type': 'application/json'},
        });
        return response.data;
    },
    updateMenu: async (menuId, menuData) => {
        const response = await apiClient.put(`/api/menus/${menuId}`, menuData);
        return response.data;
    },
    deleteMenu: async (menuId) => {
        await apiClient.delete(`/api/menus/${menuId}`);
    },

    // --- 게시글 관련 API ---
    createPost: async (postData, files) => {
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files && files.length > 0) {
            files.forEach((file) => formData.append('files', file));
        }
        const response = await apiClient.post('/api/posts', formData, {
            headers: {'Content-Type': 'multipart/form-data'},
        });
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
        return response.data;
    },
    deletePost: async (postId) => {
        const response = await apiClient.delete(`/api/posts/${postId}`);
        return response.data;
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
                size: params.size || 10,
                sort: params.sort || 'updatedAt,desc',
            },
        });
        return response.data;
    },
    getPopularPosts: async (period = 'WEEKLY') => {
        const response = await apiClient.get('/api/posts/getPopular', {params: {period}});
        return response.data;
    },

    // --- 댓글 관련 API ---
    getCommentsByPostId: async (postId) => {
        const response = await apiClient.get(`/api/comments/post/${postId}`);
        return response.data;
    },
    createComment: async (commentData) => {
        const response = await apiClient.post('/api/comments', commentData);
        return response.data;
    },
    createReply: async (parentId, replyData) => {
        const response = await apiClient.post(`/api/comments/${parentId}/reply`, replyData);
        return response.data;
    },
    updateComment: async (commentId, commentData) => {
        const response = await apiClient.put(`/api/comments/${commentId}`, commentData);
        return response.data;
    },
    deleteComment: async (commentId) => {
        await apiClient.delete(`/api/comments/${commentId}`);
    },

    // --- 파일 관련 API ---
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
        return response.data;
    },

    // --- 알림 관련 API ---
    getNotifications: async () => {
        const response = await apiClient.get('/api/notifications');
        return response.data;
    },
    markNotificationAsRead: async (notificationId) => {
        const response = await apiClient.put(`/api/notifications/${notificationId}/read`, {});
        return response; // 응답 본문이 없을 수 있음
    },
    markAllNotificationsAsRead: async () => {
        const response = await apiClient.put('/api/notifications/read-all', {});
        return response; // 응답 본문이 없을 수 있음
    },

    // --- 광고 관련 API 함수 ---
    // 활성화된 광고 목록 조회
    getActiveAdvertisements: async () => {
        try {
            // GET /api/advertisements/active 엔드포인트 사용
            const response = await apiClient.get('/api/advertisements/active');
            return response.data;
        } catch (error) {
            console.error("Error fetching active advertisements:", error);
            throw error;
        }
    },

    // [수정] 광고 생성 API (FormData 사용)
    createAdvertisementFormData: async (formData) => { // 함수 이름 수정 및 추가
        try {
            // POST /api/advertisements 엔드포인트 사용 (AdvertisementController에 정의된 경로)
            const response = await apiClient.post('/api/advertisements', formData, {
                headers: {
                    // 'Content-Type': 'multipart/form-data' // Axios가 FormData 사용 시 자동으로 설정하므로 명시적 설정은 보통 불필요
                }
            });
            return response.data; // 생성된 광고 DTO 반환 예상
        } catch (error) {
            console.error("Error creating advertisement with FormData:", error);
            throw error; // 오류 전파
        }
    },

    // [추가] 광고 수정 API (FormData 사용)
    updateAdvertisement: async (id, formData) => {
        try {
            // PUT /api/advertisements/{id} 엔드포인트 사용 (AdvertisementController에 정의된 경로)
            const response = await apiClient.put(`/api/advertisements/${id}`, formData, {
                headers: {
                    // 'Content-Type': 'multipart/form-data' // 자동 설정
                }
            });
            return response.data; // 수정된 광고 DTO 반환 예상
        } catch (error) {
            console.error(`Error updating advertisement ${id} with FormData:`, error);
            throw error; // 오류 전파
        }
    },

    // [추가] 광고 삭제 API (필요시)
    deleteAdvertisement: async (id) => {
        try {
            // DELETE /api/advertisements/{id} 엔드포인트 사용 (AdvertisementController에 정의된 경로)
            const response = await apiClient.delete(`/api/advertisements/${id}`);
            // 성공 시 204 No Content 응답이 올 수 있음
            return response.status === 204 ? {} : response.data;
        } catch (error) {
            console.error(`Error deleting advertisement ${id}:`, error);
            throw error;
        }
    },
    // --- ---

    // --- 유틸리티 함수들 ---
    getStoredUser: getStoredUser,
    setStoredUser: setStoredUser,
    clearAuthData: clearAuthData,
    getAccessToken: getAccessToken,
    getRefreshToken: getRefreshToken,
};

// authService 객체 export
export default authService;
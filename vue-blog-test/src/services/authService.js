import axios from 'axios';

const API_URL = 'http://localhost:8080'; // 백엔드 API URL
const FRONTEND_URL = 'http://localhost:3000'; // 프론트엔드 URL

axios.defaults.withCredentials = true; // 인증 정보 포함 요청 설정

const authService = {
    // 로그인
    login: async (email, password) => {
        try {
            const response = await axios.post(
                `${API_URL}/login`,
                new URLSearchParams({ email, password }),
                { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
            );
            const { accessToken, refreshToken, username, userId } = response.data;
            console.log('로그인 응답:', response.data);
            const user = { id: userId, username };
            localStorage.setItem('accessToken', accessToken);
            localStorage.setItem('refreshToken', refreshToken);
            authService.setStoredUser(user);
            axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            return response.data;
        } catch (error) {
            console.error('로그인 실패:', error);
            throw error;
        }
    },

    // 블로그 ID로 블로그 조회
    getBlogById: async (blogId) => {
        const token = localStorage.getItem('accessToken');
        console.log('getBlogById 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/id/${blogId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('블로그 ID 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('블로그 ID 조회 실패:', error);
            throw error;
        }
    },

    // 사용자 ID로 블로그 조회
    getBlogByUserId: async () => {
        const token = localStorage.getItem('accessToken');
        console.log('getBlogByUserId 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/me`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('사용자 블로그 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('사용자 블로그 조회 실패:', error);
            return null;
        }
    },

    // 블로그 ID로 메뉴 조회
    getMenusByBlogId: async (blogId) => {
        const token = localStorage.getItem('accessToken');
        console.log('getMenusByBlogId 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/menus/blog/${blogId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('메뉴 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('메뉴 조회 실패:', error);
            throw error;
        }
    },

    // 회원가입
    signup: async (userData) => {
        const response = await axios.post(`${API_URL}/api/users/signup`, userData);
        console.log('회원가입 응답:', response.data);
        return response.data;
    },

    // 현재 사용자 정보 조회
    getCurrentUser: async () => {
        const token = localStorage.getItem('accessToken');
        console.log('getCurrentUser 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/users/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('현재 사용자 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('현재 사용자 조회 실패:', error);
            throw error;
        }
    },

    // 사용자 정보 수정
    updateUserProfile: async (username, userData) => {
        const token = localStorage.getItem('accessToken');
        console.log('updateUserProfile 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.put(`${API_URL}/api/users/profile/${username}`, userData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('사용자 정보 수정 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('사용자 정보 수정 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 토큰 갱신
    refreshToken: async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        console.log('토큰 갱신 시도:', refreshToken ? '리프레시 토큰 있음' : '리프레시 토큰 없음');
        if (!refreshToken) throw new Error('리프레시 토큰 없음');
        try {
            const response = await axios.post(
                `${API_URL}/api/users/refresh`,
                { refreshToken },
                { withCredentials: true }
            );
            const { accessToken } = response.data;
            console.log('갱신 응답:', response.data);
            localStorage.setItem('accessToken', accessToken);
            axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            return response;
        } catch (error) {
            console.error('토큰 갱신 실패:', error);
            throw error;
        }
    },

    // 저장된 사용자 정보 가져오기
    getStoredUser: () => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        console.log('저장된 사용자:', user);
        return user.id || user.username ? user : null;
    },

    // 사용자 정보 저장
    setStoredUser: (user) => {
        console.log('사용자 저장:', user);
        localStorage.setItem('user', JSON.stringify(user));
    },

    // 블로그 생성
    createBlog: async (blogData) => {
        const token = localStorage.getItem('accessToken');
        console.log('createBlog 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.post(`${API_URL}/api/blogs`, blogData, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('블로그 생성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('블로그 생성 실패:', error);
            throw error;
        }
    },

    // 블로그 삭제
    deleteBlog: async (blogId, userId) => {
        const token = localStorage.getItem('accessToken');
        console.log('deleteBlog 토큰:', token);
        try {
            await axios.delete(`${API_URL}/api/blogs/${blogId}`, {
                params: { userId },
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('블로그 삭제 성공');
        } catch (error) {
            console.error('블로그 삭제 실패:', error);
            throw error;
        }
    },

    // URL 가용성 확인
    checkUrlAvailability: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('checkUrlAvailability 토큰:', token);
        try {
            const response = await axios.get(
                `${API_URL}/api/blogs/check-url?url=${encodeURIComponent(url)}`,
                {headers: {Authorization: `Bearer ${token}`}}
            );
            console.log('URL 가용성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('URL 가용성 확인 실패:', error);
            throw error;
        }
    },

    // URL로 블로그 조회
    getBlogByUrl: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('getBlogByUrl 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/${url}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('URL로 블로그 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('URL로 블로그 조회 실패:', error);
            throw error;
        }
    },

    // 블로그 수정
    updateBlog: async (blogId, blogData) => {
        const token = localStorage.getItem('accessToken');
        console.log('updateBlog 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.put(`${API_URL}/api/blogs/${blogId}`, blogData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('블로그 수정 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('블로그 수정 실패:', error);
            throw error;
        }
    },

    // 블로그 URL 가용성 확인
    checkBlogUrlAvailability: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('checkBlogUrlAvailability 토큰:', token);
        try {
            const response = await axios.get(`${API_URL}/api/blogs/check-url?url=${url}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('블로그 URL 가용성 응답:', response.data);
            return response.data.available;
        } catch (error) {
            console.error('블로그 URL 가용성 확인 실패:', error);
            throw error;
        }
    },

    // 메뉴 생성
    createMenu: async (menuData) => {
        const token = localStorage.getItem('accessToken');
        console.log('createMenu 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.post(`${API_URL}/api/menus`, menuData, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            console.log('메뉴 생성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('메뉴 생성 실패:', error);
            throw error;
        }
    },

    // 메뉴 순서 업데이트
    updateMenuOrder: async (blogId, menus) => {
        const token = localStorage.getItem('accessToken');
        console.log('updateMenuOrder 토큰:', token);
        console.log('서버로 전송할 메뉴:', JSON.stringify(menus));
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.put(`${API_URL}/api/menus/${blogId}/order`, menus, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json',
                },
            });
            console.log('메뉴 순서 업데이트 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('메뉴 순서 업데이트 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 게시글 생성
    createPost: async (postData, files) => {
        const token = localStorage.getItem('accessToken');
        console.log('createPost 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files) {
            files.forEach((file) => formData.append('files', file));
        }
        try {
            console.log('요청 전송:', `${API_URL}/api/posts`);
            console.log('요청 데이터:', postData, files);
            const response = await axios.post(`${API_URL}/api/posts`, formData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('게시글 생성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('게시글 생성 실패:', error.response?.status, error.response?.data || error.message);
            if (
                error.response?.status === 401 ||
                error.response?.status === 302 ||
                error.response?.data?.includes('Please sign in')
            ) {
                console.log('인증 실패, 로그인 페이지로 이동');
                localStorage.clear();
                window.location.href = `${FRONTEND_URL}/login`;
            }
            throw error;
        }
    },

    // 메뉴 ID로 게시글 조회
    getPostsByMenuId: async (menuId) => {
        const token = localStorage.getItem('accessToken');
        console.log('getPostsByMenuId 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/posts/menu/${menuId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('메뉴 ID로 게시글 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('메뉴 ID로 게시글 조회 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 게시글 수정
    updatePost: async (postId, postData, files) => {
        const token = localStorage.getItem('accessToken');
        console.log('updatePost 토큰:', token);
        const formData = new FormData();
        formData.append('dto', new Blob([JSON.stringify(postData)], {type: 'application/json'}));
        if (files && files.length > 0) {
            files.forEach((file) => formData.append('files', file));
        }
        try {
            const response = await axios.put(`${API_URL}/api/posts/${postId}`, formData, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'multipart/form-data',
                },
            });
            console.log('게시글 수정 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('게시글 수정 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 게시글 삭제
    deletePost: async (postId, userId) => {
        const token = localStorage.getItem('accessToken');
        console.log('deletePost 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.delete(`${API_URL}/api/posts/${postId}`, {
                headers: {Authorization: `Bearer ${token}`},
                params: {userId},
            });
            console.log('게시글 삭제 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('게시글 삭제 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 게시글 ID로 조회
    getPostById: async (postId) => {
        const token = localStorage.getItem('accessToken');
        console.log('getPostById 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/posts/${postId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('게시글 ID 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('게시글 ID 조회 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 로그아웃
    logout: async () => {
        try {
            const response = await axios.get(`${API_URL}/api/users/logout`, {
                headers: {Authorization: `Bearer ${localStorage.getItem('accessToken')}`},
            });
            console.log('로그아웃 응답:', response.data);
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            delete axios.defaults.headers.common['Authorization'];
            return response.data;
        } catch (error) {
            console.error('로그아웃 실패:', error);
            throw error;
        }
    },

    getPostsByBlogIdAndMenuId: async (blogId, menuId, params = {}) => {
        const token = localStorage.getItem('accessToken');
        if (!token) throw new Error('토큰 없음');
        const url = menuId ? `${API_URL}/api/posts/blog/${blogId}/${menuId}` : `${API_URL}/api/posts/blog/${blogId}`;
        try {
            const response = await axios.get(url, {
                headers: {Authorization: `Bearer ${token}`},
                params: {
                    searchKeyword: params.searchKeyword,
                    page: params.page || 0,
                    size: params.size || 20,
                    sort: params.sort || 'updatedAt,desc',
                },
            });
            console.log('게시글 조회 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('게시글 조회 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 게시글 ID로 댓글 조회
    getCommentsByPostId: async (postId) => {
        const token = localStorage.getItem('accessToken');
        console.log('getCommentsByPostId 토큰:', token);
        if (!token) throw new Error('토큰 없음');
        try {
            const response = await axios.get(`${API_URL}/api/comments/post/${postId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('댓글 조회 응답:', JSON.stringify(response.data));
            return response.data;
        } catch (error) {
            console.error('댓글 조회 실패:', error.response?.data || error.message);
            throw error;
        }
    },

    // 댓글 생성
    createComment: async (commentData) => {
        const token = localStorage.getItem('accessToken');
        console.log('createComment 토큰:', token);
        try {
            const response = await axios.post(`${API_URL}/api/comments`, commentData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('댓글 생성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('댓글 생성 실패:', error);
            throw error;
        }
    },

    // 답글 생성
    createReply: async (parentId, replyData) => {
        const token = localStorage.getItem('accessToken');
        console.log('createReply 토큰:', token);
        try {
            const response = await axios.post(`${API_URL}/api/comments/${parentId}/reply`, replyData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('답글 생성 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('답글 생성 실패:', error);
            throw error;
        }
    },
};

// Axios 응답 인터셉터 설정
axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const isUnauthorized =
            error.response?.status === 401 ||
            error.response?.status === 302 ||
            (error.response?.data &&
                typeof error.response.data === 'string' &&
                error.response.data.includes('Please sign in'));

        if (isUnauthorized && !originalRequest._retry) {
            originalRequest._retry = true;
            console.log('인증 실패 감지:', error.response?.status, error.response?.data);
            try {
                const response = await authService.refreshToken();
                const { accessToken } = response.data;
                console.log('새 토큰:', accessToken);
                localStorage.setItem('accessToken', accessToken);
                axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
                console.log('토큰 갱신 후 요청 재시도');
                return axios(originalRequest);
            } catch (refreshError) {
                console.error('토큰 갱신 실패:', refreshError);
                localStorage.clear();
                window.location.href = `${FRONTEND_URL}/login`;
                return Promise.reject(refreshError);
            }
        }
        console.error('요청 실패:', error.message);
        return Promise.reject(error);
    }
);

export default authService;
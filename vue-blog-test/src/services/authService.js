import axios from 'axios';

const API_URL = 'http://localhost:8080';
const FRONTEND_URL = 'http://localhost:3000';

axios.defaults.withCredentials = true;

const authService = {
    login: async (email, password) => {
        const response = await axios.post(
            `${API_URL}/login`,
            new URLSearchParams({ email, password }),
            { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
        );
        const { accessToken, refreshToken, username, userId } = response.data;
        console.log('Login response:', response.data);

        const user = { id: userId, username };
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        authService.setStoredUser(user);
        return response.data;
    },

    signup: async (userData) => {
        const response = await axios.post(`${API_URL}/api/users/signup`, userData);
        return response.data;
    },

    logout: async () => {
        const response = await axios.post(`${API_URL}/api/users/logout`);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        return response.data;
    },

    getCurrentUser: async () => {
        const response = await axios.get(`${API_URL}/api/users/me`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
        return response.data;
    },

    handleOAuth2Redirect: async (router) => {
        try {
            // 서버에서 이미 토큰을 쿠키에 설정했으므로, 사용자 정보만 확인
            const response = await authService.getCurrentUser();
            const { username, id } = response;
            console.log('OAuth2 redirect response:', response);

            const user = { id, username };
            authService.setStoredUser(user);

            if (router) {
                router.push('/'); // 홈으로 이동
            } else {
                window.location.href = `${FRONTEND_URL}/`;
            }
            return response;
        } catch (error) {
            console.error('OAuth2 redirect failed:', error);
            if (router) {
                router.push('/login');
            } else {
                window.location.href = `${FRONTEND_URL}/login`;
            }
            throw error;
        }
    },

    refreshToken: async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        console.log('Refreshing token with:', refreshToken ? 'present' : 'missing');
        if (!refreshToken) throw new Error('No refresh token available');

        const response = await axios.post(
            `${API_URL}/api/users/refresh`,
            { refreshToken },
            { withCredentials: true }
        );
        const { accessToken } = response.data;
        console.log('Refresh response:', response.data);

        localStorage.setItem('accessToken', accessToken);
        return response;
    },

    getStoredUser: () => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        console.log('Raw stored user:', user);
        return user.id || user.username ? user : null;
    },

    setStoredUser: (user) => {
        console.log('Setting stored user:', user);
        localStorage.setItem('user', JSON.stringify(user));
    },

    createBlog: async (blogData, userId) => {
        const response = await axios.post(`${API_URL}/api/blogs`, blogData, {
            params: { userId },
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
        return response.data;
    },

    updateBlog: async (blogId, blogData, userId) => {
        const response = await axios.put(`${API_URL}/api/blogs/${blogId}`, blogData, {
            params: { userId },
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
        return response.data;
    },

    deleteBlog: async (blogId, userId) => {
        await axios.delete(`${API_URL}/api/blogs/${blogId}`, {
            params: { userId },
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
    },

    getBlogByUserId: async (userId) => {
        const response = await axios.get(`${API_URL}/api/blogs/user/${userId}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
        return response.data;
    },

    checkUrlAvailability: async (url) => {
        const response = await axios.get(
            `${API_URL}/api/blogs/check-url?url=${encodeURIComponent(url)}`,
            {
                headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
            }
        );
        return response.data;
    },

    getBlogByUrl: async (url) => {
        const response = await axios.get(`${API_URL}/api/blogs/${url}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
        });
        return response.data;
    },
};

axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response && error.response.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            console.log('401 detected, attempting token refresh');
            try {
                const response = await authService.refreshToken();
                const { accessToken } = response.data;
                localStorage.setItem('accessToken', accessToken);
                axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
                console.log('Token refreshed, retrying request');
                return axios(originalRequest);
            } catch (refreshError) {
                console.error('Refresh token failed:', refreshError);
                localStorage.clear();
                window.location.href = `${FRONTEND_URL}/login`;
                return Promise.reject(refreshError);
            }
        }
        console.error('Request failed:', error.message);
        return Promise.reject(error);
    }
);

export default authService;
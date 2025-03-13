import axios from 'axios';

const API_URL = 'http://localhost:8080';
const FRONTEND_URL = 'http://localhost:3000';

axios.defaults.withCredentials = true;

const authService = {
    // 일반 로그인
    login: async (email, password) => {
        try {
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
        } catch (error) {
            console.error('Login failed:', error);
            throw error;
        }
    },

    signup: async (userData) => {
        const response = await axios.post(`${API_URL}/api/users/signup`, userData);
        console.log('Signup response:', response.data);
        return response.data;
    },

    logout: async () => {
        try {
            const response = await axios.post(`${API_URL}/api/users/logout`);
            console.log('Logout response:', response.data);
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            localStorage.removeItem('user');
            return response.data;
        } catch (error) {
            console.error('Logout failed:', error);
            throw error;
        }
    },

    getCurrentUser: async () => {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.log('No access token found in localStorage');
            throw new Error('No access token available');
        }
        try {
            const response = await axios.get(`${API_URL}/api/users/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Get current user response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get current user failed:', error);
            throw error;
        }
    },

    refreshToken: async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        console.log('Refreshing token with:', refreshToken ? 'present' : 'missing');
        if (!refreshToken) throw new Error('No refresh token available');
        try {
            const response = await axios.post(
                `${API_URL}/api/users/refresh`,
                { refreshToken },
                { withCredentials: true }
            );
            const { accessToken } = response.data;
            console.log('Refresh response:', response.data);
            localStorage.setItem('accessToken', accessToken);
            return response;
        } catch (error) {
            console.error('Refresh token failed:', error);
            throw error;
        }
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

// 블로그 생성
    createBlog: async (blogData) => {
        const token = localStorage.getItem('accessToken');
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.post(`${API_URL}/api/blogs`, blogData, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Blog created:', response.data);
            return response.data;
        } catch (error) {
            console.error('Blog creation failed:', error);
            throw error;
        }
    },

    updateBlog: async (blogId, blogData, userId) => {
        const token = localStorage.getItem('accessToken');
        try {
            const response = await axios.patch(`${API_URL}/api/blogs/${blogId}`, blogData, {
                params: { userId },
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Update blog response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Update blog failed:', error);
            throw error;
        }
    },

    deleteBlog: async (blogId, userId) => {
        const token = localStorage.getItem('accessToken');
        try {
            await axios.delete(`${API_URL}/api/blogs/${blogId}`, {
                params: { userId },
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Blog deleted successfully');
        } catch (error) {
            console.error('Delete blog failed:', error);
            throw error;
        }
    },
    async getBlogByUserId() {
        const token = localStorage.getItem('accessToken');
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Get blog by userId response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get blog by userId failed:', error);
            return null; // 블로그가 없으면 null 반환
        }
    },

    checkUrlAvailability: async (url) => {
        const token = localStorage.getItem('accessToken');
        try {
            const response = await axios.get(
                `${API_URL}/api/blogs/check-url?url=${encodeURIComponent(url)}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            console.log('Check URL availability response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Check URL availability failed:', error);
            throw error;
        }
    },
    async getBlogByUrl(url) {
        const token = localStorage.getItem('accessToken');
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/${url}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Get blog by URL response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get blog by URL failed:', error);
            throw error;
        }
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
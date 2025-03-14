import axios from 'axios';

const API_URL = 'http://localhost:8080';
const FRONTEND_URL = 'http://localhost:3000';

axios.defaults.withCredentials = true;

const authService = {
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
            axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            return response.data;
        } catch (error) {
            console.error('Login failed:', error);
            throw error;
        }
    },

    getBlogById: async (blogId) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for getBlogById:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/id/${blogId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Get blog by ID response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get blog by ID failed:', error);
            throw error;
        }
    },

    getBlogByUserId: async () => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for getBlogByUserId:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/me`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Get blog by userId response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get blog by userId failed:', error);
            return null;
        }
    },

    getMenusByBlogId: async (blogId) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for getMenusByBlogId:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/menus/blog/${blogId}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Get menus response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get menus failed:', error);
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
            delete axios.defaults.headers.common['Authorization'];
            return response.data;
        } catch (error) {
            console.error('Logout failed:', error);
            throw error;
        }
    },

    getCurrentUser: async () => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for getCurrentUser:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/users/me`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Get current user response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get current user failed:', error);
            throw error; // 인터셉터가 처리
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
            axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
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
        console.log('Access Token for createBlog:', token);
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

    deleteBlog: async (blogId, userId) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for deleteBlog:', token);
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

    checkUrlAvailability: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for checkUrlAvailability:', token);
        try {
            const response = await axios.get(
                `${API_URL}/api/blogs/check-url?url=${encodeURIComponent(url)}`,
                {headers: {Authorization: `Bearer ${token}`}}
            );
            console.log('Check URL availability response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Check URL availability failed:', error);
            throw error;
        }
    },

    getBlogByUrl: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for getBlogByUrl:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.get(`${API_URL}/api/blogs/${url}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Get blog by URL response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Get blog by URL failed:', error);
            throw error;
        }
    },


    updateBlog: async (blogId, blogData) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for updateBlog:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.put(`${API_URL}/api/blogs/${blogId}`, blogData, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Update blog response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Update blog failed:', error);
            throw error;
        }
    },

    checkBlogUrlAvailability: async (url) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for checkBlogUrlAvailability:', token);
        try {
            const response = await axios.get(`${API_URL}/api/blogs/check-url?url=${url}`, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Check blog URL response:', response.data);
            return response.data.available;
        } catch (error) {
            console.error('Check blog URL failed:', error);
            throw error;
        }
    },

    createMenu: async (menuData) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for createMenu:', token);
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.post(`${API_URL}/api/menus`, menuData, {
                headers: { Authorization: `Bearer ${token}` },
            });
            console.log('Create menu response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Create menu failed:', error);
            throw error;
        }
    },

    checkMenuUrlAvailability: async (blogId, url) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for checkMenuUrlAvailability:', token);
        try {
            const response = await axios.get(`${API_URL}/api/menus/check-url?blogId=${blogId}&url=${url}`, {
                headers: {Authorization: `Bearer ${token}`},
            });
            console.log('Check menu URL response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Check menu URL failed:', error);
            throw error;
        }
    },
    async saveMenuOrder() {
        if (!this.orderChanged) {
            alert('변경된 사항이 없습니다.');
            return;
        }
        try {
            this.isLoading = true;
            await authService.updateMenuOrder(this.blog.id, this.menus);
            alert('메뉴 순서가 저장되었습니다.');
            this.orderChanged = false;
        } catch (error) {
            console.error('Failed to update menu order:', error);
            alert('메뉴 순서 저장 실패: ' + (error.response?.data?.message || error.message));
        } finally {
            this.isLoading = false;
        }
    },
    updateMenuOrder: async (blogId, menus) => {
        const token = localStorage.getItem('accessToken');
        console.log('Access Token for updateMenuOrder:', token);
        console.log('Sending menus to server:', JSON.stringify(menus));
        if (!token) throw new Error('No access token available');
        try {
            const response = await axios.put(`${API_URL}/api/menus/order/${blogId}`, menus, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
            });
            console.log('Update menu order response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Update menu order failed:', error.response?.data || error.message);
            throw error;
        }
    },
};

// Axios 인터셉터 설정
axios.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const isUnauthorized = error.response?.status === 401 ||
            (error.response?.data && typeof error.response.data === 'string' && error.response.data.includes('Please sign in'));
        if (isUnauthorized && !originalRequest._retry) {
            originalRequest._retry = true;
            console.log('401 detected or HTML response, attempting token refresh');
            try {
                const response = await authService.refreshToken();
                const { accessToken } = response.data;
                console.log('New access token:', accessToken);
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
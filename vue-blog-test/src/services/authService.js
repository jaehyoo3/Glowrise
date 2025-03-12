import axios from 'axios';

const API_URL = 'http://localhost:8080';

axios.defaults.withCredentials = true;

const authService = {
    login: async (email, password) => {
        const response = await axios.post(`${API_URL}/login`, new URLSearchParams({ email, password }), {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
        const { accessToken, refreshToken, username, userId } = response.data;
        console.log('Login response:', response.data);

        const user = {
            id: userId,
            username: username
        };

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        authService.setStoredUser(user);
        console.log('Stored user:', user);

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
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    },

    refreshToken: async () => {
        const response = await axios.post(`${API_URL}/api/users/refresh`);
        const { accessToken, refreshToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        return response.data;
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
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    },

    updateBlog: async (blogId, blogData, userId) => {
        const response = await axios.put(`${API_URL}/api/blogs/${blogId}`, blogData, {
            params: { userId },
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    },

    deleteBlog: async (blogId, userId) => {
        await axios.delete(`${API_URL}/api/blogs/${blogId}`, {
            params: { userId },
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
    },

    getBlogByUserId: async (userId) => {
        const response = await axios.get(`${API_URL}/api/blogs/user/${userId}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    },

    checkUrlAvailability: async (url) => {
        const response = await axios.get(`${API_URL}/api/blogs/check-url?url=${encodeURIComponent(url)}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    },

    getBlogByUrl: async (url) => {
        const response = await axios.get(`${API_URL}/api/blogs/${url}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
        });
        return response.data;
    }
};

axios.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;
        if (error.response && error.response.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            const { accessToken } = await authService.refreshToken();
            axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
            originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
            return axios(originalRequest);
        }
        return Promise.reject(error);
    }
);

export default authService;
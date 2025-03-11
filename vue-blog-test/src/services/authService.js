import axios from 'axios';

const API_URL = 'http://localhost:8080';

axios.defaults.withCredentials = true;

const authService = {
    login: async (email, password) => {
        const response = await axios.post(`${API_URL}/login`, new URLSearchParams({
            email,
            password
        }), {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        });
        const { accessToken, refreshToken, username } = response.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('username', username);
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
        localStorage.removeItem('username');
        return response.data;
    },

    getCurrentUser: async () => {
        const response = await axios.get(`${API_URL}/api/users/me`);
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
        const username = localStorage.getItem('username');
        return username ? { username } : null;
    },

    setStoredUser: (user) => {
        localStorage.setItem('username', user.username);
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
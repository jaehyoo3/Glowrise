import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true, // 쿠키를 자동으로 포함
});

const multipartApi = axios.create({
    baseURL: 'http://localhost:8080/api',
    headers: { 'Content-Type': 'multipart/form-data' },
    withCredentials: true,
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('accessToken'); // 로컬 스토리지 보조
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
}, error => Promise.reject(error));

multipartApi.interceptors.request.use(config => {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
}, error => Promise.reject(error));

// 토큰 갱신 로직 (선택)
api.interceptors.response.use(response => response, async error => {
    if (error.response?.status === 401) {
        try {
            const response = await api.post('/users/refresh', {}, { withCredentials: true });
            localStorage.setItem('accessToken', response.data.accessToken);
            error.config.headers.Authorization = `Bearer ${response.data.accessToken}`;
            return api(error.config);
        } catch (refreshError) {
            console.error('토큰 갱신 실패:', refreshError);
            return Promise.reject(refreshError);
        }
    }
    return Promise.reject(error);
});

export { api, multipartApi };
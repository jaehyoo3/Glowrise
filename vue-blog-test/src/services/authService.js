import axios from 'axios';

const API_URL = 'http://localhost:8080';

const apiClient = axios.create({
    baseURL: API_URL,
    withCredentials: true,
});

const getAccessToken = () => localStorage.getItem('accessToken');
const getRefreshToken = () => localStorage.getItem('refreshToken');

const setTokens = (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
    }
};

const getStoredUser = () => {
    try {
        const userString = localStorage.getItem('user');
        return userString ? JSON.parse(userString) : null;
    } catch (error) {
        localStorage.removeItem('user');
        return null;
    }
};

const setStoredUser = (user) => {
    if (user && (user.id || user.username)) {
        localStorage.setItem('user', JSON.stringify(user));
    } else {
        localStorage.removeItem('user');
    }
};

const clearAuthData = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
};

const handleLogoutRedirect = () => {
    if (typeof window !== 'undefined') {
        // window.location.href = '/login';
    }
}

apiClient.interceptors.request.use(
    (config) => {
        const token = getAccessToken();
        if (token && !config.url.endsWith('/api/users/refresh')) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.endsWith('/api/users/refresh')) {
            originalRequest._retry = true;
            const currentRefreshToken = getRefreshToken();

            if (!currentRefreshToken) {
                clearAuthData();
                handleLogoutRedirect();
                return Promise.reject(error);
            }

            try {
                const refreshResponse = await apiClient.post('/api/users/refresh', {refreshToken: currentRefreshToken});
                const {accessToken} = refreshResponse.data;

                setTokens(accessToken);

                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
                return apiClient(originalRequest);

            } catch (refreshError) {
                clearAuthData();
                handleLogoutRedirect();
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
);

const authService = {
    login: async (email, password) => {
        const response = await apiClient.post(
            '/login',
            new URLSearchParams({email, password}),
            {headers: {'Content-Type': 'application/x-www-form-urlencoded'}}
        );
        const {accessToken, refreshToken, username, userId} = response.data;
        const user = {id: userId, username};
        setTokens(accessToken, refreshToken);
        setStoredUser(user);
        return response.data;
    },

    signup: async (userData) => {
        const response = await apiClient.post('/api/users/signup', userData);
        return response.data;
    },

    logout: async () => {
        try {
            await apiClient.get('/api/users/logout');
        } catch (error) {
            // Handle error silently or propagate
        } finally {
            clearAuthData();
        }
    },

    getCurrentUser: async () => {
        try {
            const response = await apiClient.get('/api/users/me');
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    updateUserProfile: async (username, userData) => {
        const response = await apiClient.put(`/api/users/profile/${username}`, userData);
        return response.data;
    },

    updateUserNickname: async (nickname) => {
        const response = await apiClient.put('/api/users/me/nickname', {nickName: nickname});
        return response.data;
    },

    getBlogById: async (blogId) => {
        const response = await apiClient.get(`/api/blogs/id/${blogId}`);
        return response.data;
    },

    getBlogByUserId: async () => {
        try {
            const response = await apiClient.get('/api/blogs/me');
            return response.data;
        } catch (error) {
            throw error;
        }
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

    getNotifications: async () => {
        const response = await apiClient.get('/api/notifications');
        return response.data;
    },
    markNotificationAsRead: async (notificationId) => {
        const response = await apiClient.put(`/api/notifications/${notificationId}/read`, {});
        return response;
    },
    markAllNotificationsAsRead: async () => {
        const response = await apiClient.put('/api/notifications/read-all', {});
        return response;
    },

    getStoredUser: getStoredUser,
    setStoredUser: setStoredUser,
    clearAuthData: clearAuthData,
    getAccessToken: getAccessToken,
    getRefreshToken: getRefreshToken,
};

export default authService;
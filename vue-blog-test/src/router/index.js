// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import HomeView from '@/views/HomeView.vue';
import authService from '@/services/authService';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomeView,
        meta: { requiresAuth: true }
    },
    {
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: { guest: true }
    },
    // 리다이렉트 경로 (OAuth 콜백용)
    {
        path: '/oauth2/redirect',
        name: 'OAuth2Redirect',
        component: () => import('@/views/OAuth2RedirectHandler.vue')
    },
    // 기타 경로들...
    {
        path: '/:catchAll(.*)',
        redirect: '/'
    }
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

// 네비게이션 가드 설정
router.beforeEach(async (to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const isGuestRoute = to.matched.some(record => record.meta.guest);

    // 로컬 스토리지에서 사용자 정보 확인
    const user = authService.getStoredUser();

    // 인증이 필요한 경로인 경우
    if (requiresAuth) {
        if (!user) {
            // 사용자 정보가 없으면 로그인 페이지로
            next({ name: 'Login' });
        } else {
            try {
                // 서버에서 현재 사용자 정보 확인 (토큰 유효성 검증)
                await authService.getCurrentUser();
                next(); // 인증 성공 시 요청한 페이지로
            } catch (error) {
                // 인증 실패 시 로그인 페이지로
                localStorage.removeItem('user');
                next({ name: 'Login' });
            }
        }
    }
    // 게스트 전용 경로인 경우 (로그인, 회원가입 등)
    else if (isGuestRoute) {
        if (user) {
            // 이미 로그인된 사용자는 홈으로
            next({ name: 'Home' });
        } else {
            next(); // 인증되지 않은 사용자는 게스트 페이지 접근 가능
        }
    }
    // 그 외 경로는 모두 허용
    else {
        next();
    }
});

export default router;
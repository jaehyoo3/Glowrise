import {createRouter, createWebHistory} from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import BlogCreateView from '@/views/BlogCreateView.vue';
import BlogEditView from '@/views/BlogEditView.vue';
import BlogView from '@/views/BlogView.vue';
import PostCreate from '@/views/PostCreate.vue';
import PostDetail from '@/views/PostDetail.vue';
import PostEdit from '@/views/PostEdit.vue';
import UserProfileEdit from '@/views/UserProfileEdit.vue';
import authService from '@/services/authService';

const routes = [
    {path: '/', name: 'Home', component: HomeView}, // requiresAuth 제거
    { path: '/oauth2/redirect', name: 'OAuth2Redirect', component: () => import('@/views/OAuth2RedirectHandler.vue') },
    { path: '/blog/create', name: 'BlogCreate', component: BlogCreateView, meta: { requiresAuth: true } },
    { path: '/blog/edit', name: 'BlogEdit', component: BlogEditView, meta: { requiresAuth: true } },
    {path: '/blog/edit/:id', component: BlogEditView, meta: {requiresAuth: true}},
    {path: '/post/edit/:postId', name: 'PostEdit', component: PostEdit, props: true, meta: {requiresAuth: true}},
    {path: '/:blogUrl/post/create', name: 'PostCreate', component: PostCreate, props: true, meta: {requiresAuth: true}},
    {
        path: '/:blogUrl/:menuId/:postId',
        name: 'PostDetail',
        component: PostDetail,
        props: true,
        // meta: { requiresAuth: true } // 비회원도 접근 가능하도록 제거
    },
    {path: '/:blogUrl', name: 'Blog', component: BlogView, props: true},
    {
        path: '/:blogUrl/:menuId/edit/:postId',
        name: 'PostEditWithMenu',
        component: PostEdit,
        props: true,
        meta: {requiresAuth: true}
    },
    {path: '/profile/edit', name: 'UserProfileEdit', component: UserProfileEdit, meta: {requiresAuth: true}},
    {
        path: '/profile/set-nickname', // OAuth2RedirectHandler에서 사용하는 경로와 일치
        name: 'SetNickname',          // 라우트 이름 (고유해야 함)
        // SetNickname.vue 컴포넌트를 lazy-loading 방식으로 로드
        component: () => import('@/views/SetNickname.vue'),
        meta: {requiresAuth: true} // 로그인한 사용자만 접근 가능하도록 설정
    },

    { path: '/:catchAll(.*)', redirect: '/' }
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

router.beforeEach(async (to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    // const isGuestRoute = to.matched.some(record => record.meta.guest); // guest 메타 제거했으므로 주석 처리 또는 삭제
    const user = authService.getStoredUser();

    console.log('Navigating to:', to.fullPath, 'Requires Auth:', requiresAuth, 'User:', !!user);

    if (requiresAuth) {
        if (!user) {
            console.log('No user for protected route, redirecting (implicitly handled by lack of login)');
            // 로그인 페이지가 없으므로 홈으로 보내거나, NavBar에서 모달을 띄우도록 유도해야 함
            // 여기서는 일단 홈으로 보내고, NavBar가 모달을 띄우도록 하는 것이 자연스러움
            // 또는 alert('로그인이 필요합니다.'); next(false); 등으로 막기
            alert('로그인이 필요한 서비스입니다.');
            next('/'); // 또는 next(false);
        } else {
            // 토큰 유효성 검사 (선택적이지만 권장)
            try {
                await authService.getCurrentUser(); // 토큰 갱신 또는 유효성 확인
                console.log('Auth check passed, proceeding to:', to.fullPath);
                next(); // 인증 통과
            } catch (error) {
                console.error('Auth check failed during navigation:', error);
                alert('세션이 만료되었거나 유효하지 않습니다. 다시 로그인해주세요.');
                // authService.logout()은 인터셉터에서 처리될 수 있음
                next('/'); // 오류 시 홈으로
            }
        }
    } else { // 인증 불필요 경로
        console.log('No auth required, proceeding to:', to.fullPath);
        next();
    }
});

export default router;
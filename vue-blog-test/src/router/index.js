import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import HomeView from '@/views/HomeView.vue';
import BlogCreateView from '@/views/BlogCreateView.vue';
import BlogEditView from '@/views/BlogEditView.vue';
import BlogView from '@/views/BlogView.vue';
import PostCreate from '@/views/PostCreate.vue';
import PostDetail from '@/views/PostDetail.vue';
import PostEdit from '@/views/PostEdit.vue';
import UserProfileEdit from '@/views/UserProfileEdit.vue'; // 새 컴포넌트 추가
import authService from '@/services/authService';

const routes = [
    { path: '/', name: 'Home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/login', name: 'Login', component: LoginView, meta: { guest: true } },
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
        meta: {requiresAuth: true}
    },
    {path: '/:blogUrl/:menuId', name: 'BlogMenu', component: BlogView, props: true},
    {path: '/:blogUrl', name: 'Blog', component: BlogView, props: true},
    {
        path: '/:blogUrl/:menuId/edit/:postId',
        name: 'PostEditWithMenu',
        component: PostEdit,
        props: true,
        meta: {requiresAuth: true}
    },
    // 사용자 정보 수정 경로 추가
    {path: '/profile/edit', name: 'UserProfileEdit', component: UserProfileEdit, meta: {requiresAuth: true}},
    { path: '/:catchAll(.*)', redirect: '/' }
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

router.beforeEach(async (to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const isGuestRoute = to.matched.some(record => record.meta.guest);
    const user = authService.getStoredUser();

    console.log('Navigating to:', to.fullPath, 'Requires Auth:', requiresAuth, 'User:', user);

    if (requiresAuth) {
        if (!user) {
            console.log('No user, redirecting to login');
            next({ name: 'Login' });
        } else {
            try {
                await authService.getCurrentUser();
                console.log('Auth check passed, proceeding to:', to.fullPath);
                next();
            } catch (error) {
                console.error('Auth check failed:', error);
                localStorage.removeItem('username');
                next({ name: 'Login' });
            }
        }
    } else if (isGuestRoute) {
        if (user) {
            console.log('User exists, redirecting to home from guest route');
            next({ name: 'Home' });
        } else {
            next();
        }
    } else {
        console.log('No auth required, proceeding to:', to.fullPath);
        next();
    }
});

export default router;
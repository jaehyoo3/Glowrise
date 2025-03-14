import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import HomeView from '@/views/HomeView.vue';
import BlogCreateView from '@/views/BlogCreateView.vue';
import BlogEditView from '@/views/BlogEditView.vue';
import BlogView from '@/views/BlogView.vue';
import authService from '@/services/authService';

const routes = [
    { path: '/', name: 'Home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/login', name: 'Login', component: LoginView, meta: { guest: true } },
    { path: '/oauth2/redirect', name: 'OAuth2Redirect', component: () => import('@/views/OAuth2RedirectHandler.vue') },
    { path: '/blog/create', name: 'BlogCreate', component: BlogCreateView, meta: { requiresAuth: true } },
    { path: '/blog/edit', name: 'BlogEdit', component: BlogEditView, meta: { requiresAuth: true } },
    {path: '/blog/edit/:id', component: () => import('../views/BlogEditView.vue')}, // ID 기반 경로
    { path: '/blog/:url', name: 'Blog', component: BlogView },
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

    if (requiresAuth) {
        if (!user) {
            next({ name: 'Login' });
        } else {
            try {
                await authService.getCurrentUser();
                next();
            } catch (error) {
                localStorage.removeItem('username');
                next({ name: 'Login' });
            }
        }
    } else if (isGuestRoute) {
        if (user) {
            next({ name: 'Home' });
        } else {
            next();
        }
    } else {
        next();
    }
});

export default router;
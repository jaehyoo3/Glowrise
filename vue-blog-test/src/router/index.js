// Glowrise/vue-blog-test/src/router/index.js
// (검색 라우트 추가된 전체 파일)

import {createRouter, createWebHistory} from 'vue-router';
import authService from '@/services/authService';

// --- 기존 View 컴포넌트 Import ---
const HomeView = () => import('@/views/HomeView.vue');
const OAuth2RedirectHandler = () => import('@/views/OAuth2RedirectHandler.vue');
const BlogCreateView = () => import('@/views/BlogCreateView.vue');
const BlogEditView = () => import('@/views/BlogEditView.vue');
const BlogView = () => import('@/views/BlogView.vue');
const PostCreate = () => import('@/views/PostCreate.vue');
const PostDetail = () => import('@/views/PostDetail.vue');
const PostEdit = () => import('@/views/PostEdit.vue');
const UserProfileEdit = () => import('@/views/UserProfileEdit.vue');
const SetNickname = () => import('@/views/SetNickname.vue');
const SearchResultPage = () => import('@/views/SearchResultPage.vue');

const routes = [
    {path: '/', name: 'Home', component: HomeView},
    {path: '/oauth2/redirect', name: 'OAuth2Redirect', component: OAuth2RedirectHandler},

    {
        path: '/search',
        name: 'search', // 라우트 이름 설정
        component: SearchResultPage, // 연결할 컴포넌트
        props: route => ({
            query: route.query.q || '',
            initialPage: parseInt(route.query.page) || 0
        }),
    },

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
        path: '/profile/set-nickname',
        name: 'SetNickname',
        component: SetNickname,
        meta: {requiresAuth: true}
    },
    {path: '/:catchAll(.*)', redirect: '/'}
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

router.beforeEach(async (to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const user = authService.getStoredUser();

    console.log('Navigating to:', to.fullPath, 'Requires Auth:', requiresAuth, 'User:', !!user);

    if (requiresAuth) {
        if (!user) {
            alert('로그인이 필요한 서비스입니다.');
            next('/');
        } else {

            next();
        }
    } else { // 인증 불필요 경로
        console.log('No auth required, proceeding to:', to.fullPath);
        next();
    }
});
// ----------------------------------------------------

export default router;
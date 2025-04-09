import {createRouter, createWebHistory} from 'vue-router';
import authService from '@/services/authService';

// --- 개선: 대부분의 페이지 컴포넌트를 동적 import로 변경 ---
// 이렇게 하면 해당 라우트로 처음 이동할 때만 컴포넌트 코드를 로드합니다.
// HomeView는 첫 페이지이므로 동적으로 로드하지 않을 수도 있습니다 (선택 사항).
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
// ---------------------------------------------------------

const routes = [
    {path: '/', name: 'Home', component: HomeView}, // 필요시 HomeView도 동적 import 가능
    {path: '/oauth2/redirect', name: 'OAuth2Redirect', component: OAuth2RedirectHandler},
    { path: '/blog/create', name: 'BlogCreate', component: BlogCreateView, meta: { requiresAuth: true } },
    { path: '/blog/edit', name: 'BlogEdit', component: BlogEditView, meta: { requiresAuth: true } },
    {path: '/blog/edit/:id', component: BlogEditView, meta: {requiresAuth: true}}, // BlogEditView 재사용
    {path: '/post/edit/:postId', name: 'PostEdit', component: PostEdit, props: true, meta: {requiresAuth: true}},
    {path: '/:blogUrl/post/create', name: 'PostCreate', component: PostCreate, props: true, meta: {requiresAuth: true}},
    {
        path: '/:blogUrl/:menuId/:postId',
        name: 'PostDetail',
        component: PostDetail,
        props: true,
        // 비회원도 접근 가능
    },
    {path: '/:blogUrl', name: 'Blog', component: BlogView, props: true},
    {
        path: '/:blogUrl/:menuId/edit/:postId',
        name: 'PostEditWithMenu',
        component: PostEdit, // PostEdit 컴포넌트 재사용
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
    {path: '/:catchAll(.*)', redirect: '/'} // 정의되지 않은 경로는 홈으로 리디렉션
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

// --- 개선: beforeEach에서 /api/users/me 호출 제거 ---
router.beforeEach(async (to, from, next) => {
    const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
    const user = authService.getStoredUser(); // 로컬 스토리지에서 사용자 정보 확인

    console.log('Navigating to:', to.fullPath, 'Requires Auth:', requiresAuth, 'User:', !!user);

    if (requiresAuth) {
        if (!user) {
            // 로컬 스토리지에 사용자 정보 없으면 로그인 필요
            console.log('No user data in local storage for protected route. Redirecting or blocking.');
            alert('로그인이 필요한 서비스입니다.');
            next('/'); // 홈으로 이동 (또는 로그인 페이지로 리디렉션)
        } else {
            // 로컬 스토리지에 사용자 정보 있으면 일단 통과
            // 실제 API 요청 시 토큰 문제는 Axios 인터셉터가 처리
            console.log('User data found in local storage. Proceeding to:', to.fullPath);
            next();
        }
    } else { // 인증 불필요 경로
        console.log('No auth required, proceeding to:', to.fullPath);
        next();
    }
});
// ----------------------------------------------------

export default router;
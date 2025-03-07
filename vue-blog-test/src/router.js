import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import SignUp from './components/SignUp.vue';
import LoginPage from './components/LoginPage.vue'; // 수정
import BlogList from './components/BlogList.vue';
import PostList from './components/PostList.vue';
import CommentList from './components/CommentList.vue';
import Profile from './components/Profile.vue';

const routes = [
    { path: '/', component: App, name: 'home' },
    { path: '/signup', component: SignUp, name: 'signup' },
    { path: '/login', component: LoginPage, name: 'login' }, // 수정
    { path: '/blogs', component: BlogList, name: 'blogs' },
    { path: '/posts/:menuId', component: PostList, name: 'posts' },
    { path: '/comments/:postId', component: CommentList, name: 'comments' },
    { path: '/profile/:username', component: Profile, name: 'profile' },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
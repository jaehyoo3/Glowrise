import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import store from './store'; // 생성한 Vuex 스토어 import
// --- 라이브러리 로딩 최적화 제안 (이전과 동일) ---
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.bundle.min.js'; // 주석 처리 또는 필요한 모듈만 사용
import '@fortawesome/fontawesome-free/css/all.min.css'; // 주석 처리 또는 Font Awesome Vue 사용
// -------------------------------------------------

const app = createApp(App);

app.use(router); // 라우터 등록
app.use(store);  // Vuex 스토어 등록

const token = localStorage.getItem('accessToken'); // 예시: 직접 접근

if (token) {
    console.log('앱 초기화: 토큰 감지. 사용자 정보 로드 시도...');

    store.dispatch('fetchCurrentUser');
} else {
    console.log('앱 초기화: 토큰 없음. 로그인 필요 상태.');
}
// -----------------------------------------

app.mount('#app');
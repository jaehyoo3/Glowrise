import {createApp} from 'vue'; // Vue 3 createApp 임포트
import App from './App.vue';
import router from './router';
import store from './store'; // Vuex 스토어
import axios from 'axios'; // Axios
// PrimeVue 라이브러리 및 설정 임포트
import PrimeVue from 'primevue/config';
import ToastService from 'primevue/toastservice'; // Toast 서비스
// PrimeVue CSS 임포트
import 'primevue/resources/themes/saga-blue/theme.css'; // 원하는 테마 선택
import 'primevue/resources/primevue.min.css'; // 코어 CSS
import 'primeicons/primeicons.css'; // 아이콘 CSS
// 사용할 PrimeVue 컴포넌트 임포트
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Checkbox from 'primevue/checkbox';
import Calendar from 'primevue/calendar';
import InputNumber from 'primevue/inputnumber';
import Carousel from 'primevue/carousel';
import Toast from 'primevue/toast';
import Menu from 'primevue/menu'; // NavBar에서 사용하는 Menu 임포트 추가

// Vue 앱 생성
const app = createApp(App);

// Vue 앱에 라우터와 스토어 사용 설정
app.use(router);
app.use(store);

// Vue 앱에 PrimeVue 및 관련 서비스 사용 설정
app.use(PrimeVue);
app.use(ToastService);

// Axios를 전역 속성으로 추가 (선택 사항)
app.config.globalProperties.$http = axios;

// PrimeVue 컴포넌트 전역 등록 (Vue 3 방식 + Multi-word 이름 사용)
app.component('PrimeButton', Button); // 이름을 'PrimeButton'으로 변경
app.component('PrimeDialog', Dialog); // 이름을 'PrimeDialog'으로 변경
app.component('PrimeInputText', InputText);
app.component('PrimeCheckbox', Checkbox);
app.component('PrimeCalendar', Calendar);
app.component('PrimeInputNumber', InputNumber);
app.component('PrimeCarousel', Carousel);
app.component('PrimeToast', Toast);
app.component('PrimeMenu', Menu); // Menu 컴포넌트도 등록

// Vue 앱 마운트
app.mount('#app');
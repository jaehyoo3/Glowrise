// main.js 예시 (PrimeVue 설정 부분)
import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import store from './store';
import PrimeVue from 'primevue/config';
import ConfirmationService from 'primevue/confirmationservice'; // ConfirmDialog 서비스
import ToastService from 'primevue/toastservice'; // Toast 서비스
import Tooltip from 'primevue/tooltip'; // Tooltip 디렉티브
// 필요한 PrimeVue 컴포넌트 임포트
import Dialog from 'primevue/dialog';
import TabView from 'primevue/tabview';
import TabPanel from 'primevue/tabpanel';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Checkbox from 'primevue/checkbox'; // MainAdvertisement에서 사용
import Calendar from 'primevue/calendar'; // AdvertisementModal에서 사용
import InputNumber from 'primevue/inputnumber'; // AdvertisementModal에서 사용
import InputSwitch from 'primevue/inputswitch'; // AdvertisementModal에서 사용 (새 버전 기준)
import Tag from 'primevue/tag';
import ConfirmDialog from 'primevue/confirmdialog'; // 확인 다이얼로그
import Toast from 'primevue/toast'; // 토스트 메시지
import Menu from 'primevue/menu'; // <--- PrimeMenu 임포트 추가
// CSS 임포트
import 'primevue/resources/themes/lara-light-indigo/theme.css'; // 테마
import 'primevue/resources/primevue.min.css'; // 코어 CSS
import 'primeicons/primeicons.css'; // 아이콘


const app = createApp(App);

app.use(router);
app.use(store);
app.use(PrimeVue, {ripple: true});
app.use(ConfirmationService); // ConfirmDialog 서비스 등록
app.use(ToastService); // Toast 서비스 등록

app.directive('tooltip', Tooltip); // Tooltip 디렉티브 등록

// 컴포넌트 전역 등록
app.component('PrimeDialog', Dialog);
app.component('PrimeTabView', TabView);
app.component('PrimeTabPanel', TabPanel);
app.component('PrimeDataTable', DataTable);
app.component('PrimeColumn', Column);
app.component('PrimeButton', Button);
app.component('PrimeInputText', InputText);
app.component('PrimeCheckbox', Checkbox);
app.component('PrimeCalendar', Calendar);
app.component('PrimeInputNumber', InputNumber);
app.component('PrimeInputSwitch', InputSwitch);
app.component('PrimeTag', Tag);
app.component('PrimeConfirmDialog', ConfirmDialog);
app.component('PrimeToast', Toast);
app.component('PrimeMenu', Menu); // <--- PrimeMenu 등록 추가

app.mount('#app');
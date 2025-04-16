<template>
  <PrimeDialog
      v-model:visible="localVisible"
      :closable="true"
      :modal="true"
      :style="{width: '600px'}"
      class="p-fluid"
      header="광고 관리"
      @hide="onHide"
  >
    <form ref="adForm" @submit.prevent="saveAdvertisement">
      <div class="p-field p-mb-3">
        <label for="imageUrl">이미지 URL</label>
        <PrimeInputText id="imageUrl" v-model.trim="advertisement.imageUrl" :class="{'p-invalid': submitted && !advertisement.imageUrl}" autofocus
                        placeholder="https://example.com/image.jpg"
                        required/>
        <small v-if="submitted && !advertisement.imageUrl" class="p-error">이미지 URL을 입력해주세요.</small>
      </div>

      <div class="p-field p-mb-3">
        <label for="targetUrl">클릭 시 이동 URL (선택)</label>
        <PrimeInputText id="targetUrl" v-model.trim="advertisement.targetUrl"
                        placeholder="https://example.com/target-page"/>
      </div>

      <div class="p-grid p-formgrid p-mb-3">
        <div class="p-col-12 p-md-6 p-field">
          <label for="startDate">표출 시작일시</label>
          <PrimeCalendar
              id="startDate"
              v-model="advertisement.startDate"
              :class="{'p-invalid': submitted && !advertisement.startDate}"
              :showTime="true"
              dateFormat="yy-mm-dd"
              hourFormat="24"
              placeholder="시작 날짜 및 시간 선택"
              required
          />
          <small v-if="submitted && !advertisement.startDate" class="p-error">시작일시를 선택해주세요.</small>
        </div>
        <div class="p-col-12 p-md-6 p-field">
          <label for="endDate">표출 종료일시</label>
          <PrimeCalendar
              id="endDate"
              v-model="advertisement.endDate"
              :class="{'p-invalid': submitted && !advertisement.endDate}"
              :minDate="advertisement.startDate"
              :showTime="true"
              dateFormat="yy-mm-dd"
              hourFormat="24"
              placeholder="종료 날짜 및 시간 선택"
              required
          />
          <small v-if="submitted && !advertisement.endDate" class="p-error">종료일시를 선택해주세요.</small>
          <small
              v-if="submitted && advertisement.endDate && advertisement.startDate && advertisement.endDate <= advertisement.startDate"
              class="p-error">종료일시는 시작일시보다 이후여야 합니다.</small>
        </div>
      </div>

      <div class="p-grid p-formgrid p-mb-3">
        <div class="p-col-12 p-md-6 p-field">
          <label for="displayOrder">표출 순서</label>
          <PrimeInputNumber id="displayOrder" v-model="advertisement.displayOrder" :class="{'p-invalid': submitted && advertisement.displayOrder === null}" :min="0" mode="decimal"
                            required/>
          <small v-if="submitted && advertisement.displayOrder === null" class="p-error">표출 순서를 입력해주세요.</small>
          <small class="p-d-block p-mt-1">숫자가 낮을수록 먼저 표시됩니다.</small>
        </div>
        <div class="p-col-12 p-md-6 p-field p-d-flex p-ai-center p-mt-4">
          <PrimeCheckbox v-model="advertisement.isActive" :binary="true" inputId="isActiveCheck"/>
          <label class="p-ml-2" for="isActiveCheck">활성화</label>
        </div>
      </div>

      <div v-if="errorMessage" class="p-mb-3 p-text-center">
        <small class="p-error">{{ errorMessage }}</small>
      </div>

    </form>

    <template #footer>
      <PrimeButton class="p-button-text" icon="pi pi-times" label="취소" @click="hideDialog"/>
      <PrimeButton :loading="isLoading" icon="pi pi-check" label="저장" @click="saveAdvertisement"/>
    </template>

  </PrimeDialog>
</template>

<script>
// import axios from 'axios'; // authService 사용으로 직접 호출 불필요
import authService from '@/services/authService'; // API 호출 위한 서비스 임포트
import {mapActions} from 'vuex'; // Vuex 액션 헬퍼 임포트

// PrimeVue 컴포넌트는 main.js에서 전역 등록됨
// 로컬 등록 시: import { PrimeDialog, PrimeButton, PrimeInputText, PrimeCalendar, PrimeInputNumber, PrimeCheckbox } from 'primevue/...';

export default {
  name: 'AdvertisementModal', // 컴포넌트 이름
  // components: { PrimeDialog, PrimeButton, PrimeInputText, ... }, // 로컬 등록 시
  props: {
    // 부모 컴포넌트에서 v-model:visible 로 전달받을 prop
    visible: {
      type: Boolean,
      default: false,
    }
  },
  // 부모 컴포넌트로 발생시킬 이벤트 명시 (Vue 3 권장)
  emits: ['update:visible', 'saved'],
  data() {
    // 컴포넌트 내부 데이터 상태
    return {
      advertisement: this.getDefaultAd(), // 광고 데이터 객체 (초기값 설정)
      isLoading: false, // 저장 버튼 로딩 상태
      errorMessage: '', // 오류 메시지
      submitted: false, // 폼 제출 시도 여부 (유효성 검사 표시용)
    };
  },
  computed: {
    // v-model:visible 구현을 위한 computed 속성
    localVisible: {
      // getter: 부모로부터 받은 visible prop 값 반환
      get() {
        return this.visible;
      },
      // setter: 내부에서 값이 변경될 때 부모에게 'update:visible' 이벤트 발생
      set(value) {
        this.$emit('update:visible', value);
      }
    }
  },
  methods: {
    // Vuex 스토어의 'fetchActiveAdvertisements' 액션을 메소드로 매핑
    ...mapActions(['fetchActiveAdvertisements']),

    // 광고 데이터 객체의 기본값을 반환하는 헬퍼 함수
    getDefaultAd() {
      const now = new Date(); // 현재 시간
      // 7일 후의 날짜 계산
      const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
      return {
        imageUrl: '',          // 이미지 URL 초기값
        targetUrl: '',         // 타겟 URL 초기값
        startDate: now,        // 시작일시 초기값 (현재)
        endDate: sevenDaysLater, // 종료일시 초기값 (7일 후)
        displayOrder: 0,       // 표출 순서 초기값
        isActive: true,        // 활성화 여부 초기값
      };
    },

    // '취소' 버튼 클릭 또는 Dialog 외부 클릭 등으로 모달을 닫을 때 호출
    hideDialog() {
      // computed setter를 호출하여 visible 상태 변경 및 이벤트 발생
      this.localVisible = false;
      // 폼 리셋은 Dialog의 @hide 이벤트에서 처리 (onHide 메소드)
    },

    // PrimeVue Dialog 컴포넌트가 닫힐 때 발생하는 @hide 이벤트 핸들러
    onHide() {
      this.resetForm(); // 폼 상태 초기화
      // Dialog 내부 메커니즘(ESC, 닫기 아이콘)으로 닫힐 때도
      // 부모의 상태와 동기화를 확실히 하기 위해 이벤트 발생 (v-model 사용 시 생략 가능)
      this.$emit('update:visible', false);
    },

    // 폼 데이터 및 관련 상태 초기화 메소드
    resetForm() {
      this.advertisement = this.getDefaultAd(); // 광고 데이터 초기화
      this.isLoading = false;                  // 로딩 상태 해제
      this.errorMessage = '';                  // 오류 메시지 초기화
      this.submitted = false;                  // 제출 시도 플래그 초기화
    },

    // JavaScript Date 객체를 ISO 8601 형식 문자열로 변환하는 헬퍼 함수
    // (백엔드의 LocalDateTime 형식에 맞게 'Z' 없이 로컬 시간 기준)
    formatDateToISO(date) {
      if (!date) return null; // 날짜 객체가 없으면 null 반환
      // 타임존 오프셋 계산 (분 단위 -> 밀리초 단위)
      const tzoffset = (new Date()).getTimezoneOffset() * 60000;
      // 로컬 시간 기준으로 ISO 문자열 생성 후 마지막 'Z' 제거
      const localISOTime = (new Date(date.getTime() - tzoffset)).toISOString().slice(0, -1);
      return localISOTime;
    },

    // 폼 제출 전 유효성 검사 메소드
    validateForm() {
      this.submitted = true; // 유효성 검사 메시지 표시 트리거
      // 필수 필드 값 존재 여부 확인
      if (!this.advertisement.imageUrl || !this.advertisement.startDate || !this.advertisement.endDate || this.advertisement.displayOrder === null) {
        this.errorMessage = '필수 입력 항목을 확인해주세요.';
        return false; // 유효성 검사 실패
      }
      // 종료일시가 시작일시보다 이후인지 확인
      if (this.advertisement.endDate <= this.advertisement.startDate) {
        this.errorMessage = '종료일시는 시작일시보다 이후여야 합니다.';
        return false; // 유효성 검사 실패
      }
      this.errorMessage = ''; // 모든 검사 통과 시 오류 메시지 초기화
      return true; // 유효성 검사 성공
    },

    // '저장' 버튼 클릭 시 실행되는 메소드
    async saveAdvertisement() {
      // 폼 유효성 검사 실행
      if (!this.validateForm()) {
        // 유효성 검사 실패 시 Toast 메시지 표시
        this.$toast.add({severity: 'warn', summary: '입력 오류', detail: this.errorMessage || '폼 입력을 확인해주세요.', life: 3000});
        return; // 저장 중단
      }

      this.isLoading = true; // 로딩 상태 활성화
      this.errorMessage = ''; // 오류 메시지 초기화

      // 서버로 전송할 페이로드(payload) 준비
      const payload = {
        ...this.advertisement, // 현재 폼 데이터 복사
        startDate: this.formatDateToISO(this.advertisement.startDate), // 시작일시 ISO 형식 변환
        endDate: this.formatDateToISO(this.advertisement.endDate),     // 종료일시 ISO 형식 변환
        // targetUrl이 비어있으면 null 또는 빈 문자열로 설정 (백엔드 요구사항 확인)
        targetUrl: this.advertisement.targetUrl || null
      };

      try {
        console.log('광고 저장 API 호출. 페이로드:', payload);
        // authService를 통해 광고 생성 API 호출
        await authService.createAdvertisement(payload);

        // API 호출 성공 후, Vuex 액션을 호출하여 메인 광고 목록 갱신
        await this.fetchActiveAdvertisements();

        // 부모 컴포넌트에 'saved' 이벤트 전달 (성공 처리 위임)
        this.$emit('saved');

        this.hideDialog(); // 성공적으로 저장 후 모달 닫기

      } catch (error) {
        // API 호출 실패 시 오류 처리
        console.error("광고 생성 실패:", error.response || error);
        this.errorMessage = '광고 생성 중 오류가 발생했습니다.';
        // 백엔드에서 전달된 구체적인 오류 메시지가 있다면 사용
        if (error.response && error.response.data) {
          this.errorMessage = error.response.data.message || error.response.data.detail || this.errorMessage;
        }
        // 사용자에게 Toast 메시지로 오류 알림
        this.$toast.add({severity: 'error', summary: '저장 실패', detail: this.errorMessage, life: 5000});
      } finally {
        // API 호출 완료 후 (성공/실패 관계없이) 로딩 상태 해제
        this.isLoading = false;
      }
    }
  },
  watch: {
    // 부모로부터 받은 visible prop 값이 변경될 때 감지
    visible(newVal) {
      // 모달이 새로 열릴 때(true) 폼 상태 초기화
      if (newVal) {
        this.resetForm();
      }
    },
    // 시작일시 값이 변경될 때 감지 (선택적 UX 개선)
    'advertisement.startDate'(newStartDate) {
      // 종료일시가 있고, 시작일시도 있으며, 종료일시가 시작일시보다 이전일 경우
      if (this.advertisement.endDate && newStartDate && this.advertisement.endDate < newStartDate) {
        // 종료일시를 시작일시와 동일하게 설정 (사용자가 직접 이후 날짜로 변경해야 함)
        this.advertisement.endDate = new Date(newStartDate.getTime());
      }
    }
  }
};
</script>

<style scoped>
/* PrimeFlex 유틸리티 클래스 (p-field, p-mb-3, p-grid, p-col-*, p-error 등) 활용 */

/* 폼 필드 라벨 스타일 */
.p-field label {
  font-weight: bold;
  display: block;
  margin-bottom: 0.5rem;
}

/* 체크박스와 라벨 정렬 */
.p-field .p-d-flex label {
  margin-bottom: 0; /* 기본 라벨 마진 제거 */
}

/* PrimeVue v3 Dialog 푸터 버튼 간격 스타일 */
.p-dialog .p-dialog-footer button {
  margin-left: 0.5rem; /* 왼쪽 버튼과의 간격 */
}

/* 첫 번째 버튼은 왼쪽 마진 없음 */
.p-dialog .p-dialog-footer button:first-child {
  margin-left: 0;
}

/* 필요하다면 다른 커스텀 스타일 추가 */
</style>
<template>
  <PrimeDialog
      v-model:visible="localVisible"
      :closable="true"
      :modal="true"
      :style="{width: '600px'}"
      class="p-fluid"
      :header="isEditing ? '광고 수정' : '광고 등록'"
      @hide="onHide"
  >
    <form ref="adForm" @submit.prevent="saveAdvertisement">

      <div class="p-field p-mb-3">
        <label for="imageFile">광고 이미지</label>
        <div v-if="isEditing && currentFileUrl" class="p-mb-2">
          <img :src="currentFileUrl" alt="Current Advertisement Image"
               style="max-width: 100%; height: auto; max-height: 150px; display: block; border: 1px solid #dee2e6; border-radius: 4px;">
          <small class="p-d-block p-mt-1">현재 이미지입니다. 새 파일을 업로드하면 교체됩니다.</small>
        </div>
        <input id="imageFile" ref="imageFileInput" :class="{'p-invalid': submitted && !hasImage()}" accept="image/*" class="p-inputtext"
               type="file" @change="handleFileChange">
        <div v-if="selectedFile" class="p-mt-1 p-d-flex p-ai-center">
          <span class="p-mr-2">{{ selectedFile.name }}</span>
          <PrimeButton class="p-button-text p-button-danger p-button-sm" icon="pi pi-times" label="선택 취소"
                       type="button" @click="resetFileInput"/>
        </div>
        <small v-if="submitted && !hasImage()"
               class="p-error">{{ isEditing ? '새 광고를 등록하거나 기존 이미지를 유지하려면 파일을 비워두세요.' : '이미지 파일을 선택해주세요.' }}</small>
        <small v-else class="p-d-block p-mt-1">이미지 파일을 선택하세요. (JPG, PNG, GIF 등)</small>
      </div>

      <div class="p-field p-mb-3">
        <label for="linkUrl">클릭 시 이동 URL (선택)</label>
        <PrimeInputText id="linkUrl" v-model.trim="advertisement.linkUrl"
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
              v-if="advertisement.endDate && advertisement.startDate && advertisement.endDate <= advertisement.startDate"
              class="p-error p-d-block">종료일시는 시작일시보다 이후여야 합니다.</small>
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
      <PrimeButton :label="isEditing ? '수정' : '저장'" :loading="isLoading" icon="pi pi-check" @click="saveAdvertisement"/>
    </template>

  </PrimeDialog>
</template>

<script>
// authService 또는 API 호출을 위한 서비스 임포트
import authService from '@/services/authService';
import {mapActions} from 'vuex';

// PrimeVue 컴포넌트는 전역 등록 가정
// 필요시 로컬 등록: import { PrimeDialog, PrimeButton, PrimeInputText, PrimeCalendar, PrimeInputNumber, PrimeCheckbox } from 'primevue/...';

export default {
  name: 'AdvertisementModal',
  // components: { ... }, // 로컬 등록 시
  props: {
    // 모달 표시 여부 (v-model:visible)
    visible: {
      type: Boolean,
      default: false,
    },
    // 수정할 광고 데이터 (수정 모드일 때 전달됨)
    editAdData: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'saved'], // 부모에게 전달할 이벤트
  data() {
    return {
      advertisement: this.getDefaultAd(), // 폼 데이터 객체
      selectedFile: null, // 선택된 파일 객체
      currentFileUrl: null, // 수정 시 현재 이미지 URL
      isLoading: false, // 로딩 상태
      errorMessage: '', // 오류 메시지
      submitted: false, // 폼 제출 시도 여부
    };
  },
  computed: {
    // v-model:visible 구현
    localVisible: {
      get() {
        return this.visible;
      },
      set(value) {
        this.$emit('update:visible', value);
      }
    },
    // 수정 모드인지 여부 판단
    isEditing() {
      // editAdData prop이 있고, id 속성이 존재하면 수정 모드로 간주
      return !!this.editAdData && this.editAdData.id != null;
    }
  },
  methods: {
    // Vuex 액션 매핑 (필요시)
    ...mapActions(['fetchActiveAdvertisements']),

    // 기본 광고 데이터 반환
    getDefaultAd() {
      const now = new Date();
      const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
      return {
        id: null, // ID는 수정 시에만 설정됨
        title: '', // 제목 필드가 백엔드에 있다면 추가 필요
        linkUrl: '',
        startDate: now,
        endDate: sevenDaysLater,
        displayOrder: 0,
        isActive: true,
        // fileUrl 은 DTO 매핑 후 설정됨, 여기서는 초기화 불필요
      };
    },

    // 모달 닫기
    hideDialog() {
      this.localVisible = false;
    },

    // Dialog의 @hide 이벤트 핸들러
    onHide() {
      this.resetForm();
      // v-model 사용 시 이 부분은 중복될 수 있으나 명확성을 위해 유지
      this.$emit('update:visible', false);
    },

    // 폼 및 상태 초기화
    resetForm() {
      this.advertisement = this.getDefaultAd();
      this.selectedFile = null;
      this.currentFileUrl = null; // 현재 파일 URL도 초기화
      this.isLoading = false;
      this.errorMessage = '';
      this.submitted = false;
      // 파일 입력 DOM 요소 직접 초기화 (선택 해제 시 필요)
      if (this.$refs.imageFileInput) {
        this.$refs.imageFileInput.value = '';
      }
    },

    // 파일 입력 변경 핸들러
    handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        this.selectedFile = file;
        // 이미지 미리보기 로직 (필요시 추가)
      } else {
        this.selectedFile = null;
      }
    },

    // 파일 입력 필드 초기화 (선택 취소 버튼 클릭 시)
    resetFileInput() {
      this.selectedFile = null;
      if (this.$refs.imageFileInput) {
        this.$refs.imageFileInput.value = '';
      }
    },

    // 이미지 존재 여부 확인 (유효성 검사용)
    hasImage() {
      // 수정 모드에서는 기존 이미지가 있거나 새 파일을 선택한 경우 true
      if (this.isEditing) {
        return !!this.currentFileUrl || !!this.selectedFile;
      }
      // 생성 모드에서는 새 파일을 선택한 경우 true
      return !!this.selectedFile;
    },

    // Date 객체를 ISO 8601 문자열 (백엔드 LocalDateTime 호환)로 변환
    formatDateToISO(date) {
      if (!date) return null;
      try {
        // 타임존 오프셋 고려하여 로컬 시간 기준 ISO 문자열 생성 (UTC 'Z' 제거)
        const tzoffset = date.getTimezoneOffset() * 60000;
        const localISOTime = new Date(date.getTime() - tzoffset).toISOString().slice(0, -1);
        // 초(second) 단위까지만 포함 (밀리초 제거)
        return localISOTime.split('.')[0];
      } catch (e) {
        console.error("날짜 변환 오류:", e);
        return null; // 오류 발생 시 null 반환
      }
    },

    // 폼 유효성 검사
    validateForm() {
      this.submitted = true;
      let isValid = true;

      // 필수 필드: 이미지 (생성 시 필수, 수정 시 선택적)
      if (!this.isEditing && !this.selectedFile) {
        this.errorMessage = '광고 이미지를 선택해주세요.';
        isValid = false;
      }
      // 필수 필드: 시작일시, 종료일시, 표시순서
      if (!this.advertisement.startDate || !this.advertisement.endDate || this.advertisement.displayOrder === null) {
        if (isValid) this.errorMessage = '필수 입력 항목(기간, 순서)을 확인해주세요.';
        isValid = false;
      }
      // 날짜 순서 검사
      if (this.advertisement.endDate && this.advertisement.startDate && this.advertisement.endDate <= this.advertisement.startDate) {
        if (isValid) this.errorMessage = '종료일시는 시작일시보다 이후여야 합니다.';
        isValid = false;
      }

      if (isValid) {
        this.errorMessage = ''; // 모든 검사 통과
      }
      return isValid;
    },

    // 광고 저장 또는 수정
    async saveAdvertisement() {
      if (!this.validateForm()) {
        this.$toast.add({severity: 'warn', summary: '입력 오류', detail: this.errorMessage || '폼 입력을 확인해주세요.', life: 3000});
        return;
      }

      this.isLoading = true;
      this.errorMessage = '';

      // FormData 객체 생성
      const formData = new FormData();

      // DTO 데이터 준비 (파일 제외)
      const adData = {
        // id는 수정 시 URL 경로로 전달되므로 페이로드에선 제외 가능하나, 백엔드 설계에 따라 포함될 수도 있음
        // id: this.advertisement.id,
        title: this.advertisement.title || '', // 제목 필드가 있다면 추가
        linkUrl: this.advertisement.linkUrl || null,
        displayOrder: this.advertisement.displayOrder,
        isActive: this.advertisement.isActive,
        startDate: this.formatDateToISO(this.advertisement.startDate),
        endDate: this.formatDateToISO(this.advertisement.endDate),
      };

      // FormData에 DTO 데이터를 JSON 문자열 Blob으로 추가
      formData.append('advertisement', new Blob([JSON.stringify(adData)], {type: 'application/json'}));

      // 선택된 파일이 있으면 FormData에 추가
      if (this.selectedFile) {
        formData.append('imageFile', this.selectedFile, this.selectedFile.name);
      }

      try {
        let response;
        if (this.isEditing) {
          // 수정 API 호출 (PUT)
          console.log(`광고 수정 API 호출 (ID: ${this.advertisement.id}). 페이로드:`, adData, "파일:", this.selectedFile ? this.selectedFile.name : "변경 없음");
          response = await authService.updateAdvertisement(this.advertisement.id, formData);
        } else {
          // 생성 API 호출 (POST)
          console.log('광고 생성 API 호출. 페이로드:', adData, "파일:", this.selectedFile ? this.selectedFile.name : "없음");
          response = await authService.createAdvertisementFormData(formData); // FormData 받는 메소드 호출
        }

        console.log("API 응답:", response);

        // 메인 목록 갱신 (필요시)
        // await this.fetchActiveAdvertisements();

        this.$emit('saved'); // 부모에게 저장 완료 이벤트 전달
        this.hideDialog(); // 모달 닫기
        this.$toast.add({
          severity: 'success',
          summary: '성공',
          detail: `광고가 성공적으로 ${this.isEditing ? '수정' : '등록'}되었습니다.`,
          life: 3000
        });

      } catch (error) {
        console.error(`광고 ${this.isEditing ? '수정' : '생성'} 실패:`, error.response || error);
        this.errorMessage = `광고 ${this.isEditing ? '수정' : '생성'} 중 오류가 발생했습니다.`;
        if (error.response && error.response.data) {
          this.errorMessage = error.response.data.message || error.response.data.detail || this.errorMessage;
        }
        this.$toast.add({severity: 'error', summary: '저장 실패', detail: this.errorMessage, life: 5000});
      } finally {
        this.isLoading = false;
      }
    },

    // 수정 모드일 때 폼 데이터 채우기
    populateFormForEdit(data) {
      if (!data) return;
      // 전달받은 데이터로 폼 필드 설정 (깊은 복사 또는 필요한 필드만 복사)
      this.advertisement = {
        ...this.getDefaultAd(), // 기본값으로 시작
        id: data.id,
        title: data.title || '', // 제목 필드가 있다면 사용
        linkUrl: data.linkUrl,
        displayOrder: data.displayOrder,
        isActive: data.isActive,
        // 날짜는 ISO 문자열로 오므로 Date 객체로 변환 필요
        startDate: data.startDate ? new Date(data.startDate) : null,
        endDate: data.endDate ? new Date(data.endDate) : null,
      };
      this.currentFileUrl = data.fileUrl || null; // 기존 파일 URL 설정
      this.selectedFile = null; // 수정 시작 시 새 파일 선택은 초기화
      this.submitted = false; // 유효성 검사 상태 초기화
      this.errorMessage = ''; // 오류 메시지 초기화
    }
  },
  watch: {
    // visible prop 변경 감지
    visible(newVal) {
      if (newVal) {
        // 모달이 열릴 때
        if (this.isEditing) {
          // 수정 모드이면 전달된 데이터로 폼 채우기
          this.populateFormForEdit(this.editAdData);
        } else {
          // 생성 모드이면 폼 초기화
          this.resetForm();
        }
      }
      // 닫힐 때는 onHide에서 resetForm 호출됨
    },

    // 시작일시 변경 감지 (UX 개선)
    'advertisement.startDate'(newStartDate, oldStartDate) {
      // oldStartDate 체크 추가하여 초기 로딩 시 불필요한 변경 방지
      if (oldStartDate && this.advertisement.endDate && newStartDate && this.advertisement.endDate < newStartDate) {
        this.advertisement.endDate = new Date(newStartDate.getTime());
      }
    }
  }
};
</script>

<style scoped>
/* PrimeFlex 유틸리티 클래스 사용 */

.p-field label {
  font-weight: bold;
  display: block;
  margin-bottom: 0.5rem;
}

/* 파일 입력 컨트롤 스타일 (기본 input 사용 시) */
input[type="file"].p-inputtext {
  /* PrimeInputText와 유사하게 보이도록 약간의 스타일 조정 가능 */
  border: 1px solid #ced4da;
  padding: 0.75rem;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
}

input[type="file"].p-inputtext.p-invalid {
  border-color: #e24c4c;
}

.p-field .p-d-flex label {
  margin-bottom: 0;
}

.p-dialog .p-dialog-footer button {
  margin-left: 0.5rem;
}

.p-dialog .p-dialog-footer button:first-child {
  margin-left: 0;
}
</style>
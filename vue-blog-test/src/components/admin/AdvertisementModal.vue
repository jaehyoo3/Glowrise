<template>
  <PrimeDialog
      v-model:visible="localVisible"
      :closable="true"
      :modal="true"
      :header="isEditing ? '광고 수정' : '신규 광고'"
      class="ad-modal"
      @hide="onHide"
  >
    <form ref="adForm" @submit.prevent="saveAdvertisement">
      <!-- 이미지 업로드 섹션 -->
      <div class="form-section">
        <div class="form-field">
          <label for="imageFile">광고 이미지</label>

          <!-- 현재 이미지 미리보기 (수정 모드) -->
          <div v-if="isEditing && currentFileUrl" class="image-preview-container">
            <img :src="currentFileUrl" alt="현재 광고 이미지" class="image-preview">
            <span class="image-caption">현재 이미지</span>
          </div>

          <!-- 새 이미지 미리보기 -->
          <div v-if="selectedFile" class="image-preview-container">
            <img :src="selectedFilePreviewUrl" alt="선택된 광고 이미지" class="image-preview">
            <div class="file-info">
              <span>{{ selectedFile.name }}</span>
              <PrimeButton
                  class="p-button-text p-button-rounded"
                  icon="pi pi-times"
                  type="button"
                  @click="resetFileInput"
              />
            </div>
          </div>

          <!-- 파일 업로드 영역 -->
          <div
              :class="{'p-invalid': submitted && !hasImage(), 'has-file': selectedFile}"
              class="upload-area"
              @click="triggerFileInput"
              @dragover.prevent="onDragOver"
              @dragleave.prevent="onDragLeave"
              @drop.prevent="onFileDrop"
          >
            <input
                id="imageFile"
                ref="imageFileInput"
                accept="image/*"
                class="hidden-file-input"
                type="file"
                @change="handleFileChange"
            >
            <i class="pi pi-upload upload-icon"></i>
            <span>이미지를 끌어서 놓거나 클릭하여 선택하세요</span>
          </div>

          <small v-if="submitted && !hasImage()" class="error-message">
            {{ isEditing ? '새 이미지를 선택하거나 기존 이미지를 유지하세요.' : '이미지 파일을 선택해주세요.' }}
          </small>
        </div>
      </div>

      <!-- 링크 URL 필드 -->
      <div class="form-section">
        <div class="form-field">
          <label for="linkUrl">클릭 시 이동 URL</label>
          <div class="input-with-icon">
            <i class="pi pi-link input-icon"></i>
            <PrimeInputText
                id="linkUrl"
                v-model.trim="advertisement.linkUrl"
                class="full-width"
                placeholder="https://example.com"
            />
          </div>
        </div>
      </div>

      <!-- 날짜 및 시간 설정 -->
      <div class="form-section">
        <div class="grid-layout">
          <div class="form-field">
            <label for="startDate">시작일시</label>
            <PrimeCalendar
                id="startDate"
                v-model="advertisement.startDate"
                :class="{'p-invalid': submitted && !advertisement.startDate}"
                :showTime="true"
                dateFormat="yy-mm-dd"
                hourFormat="24"
                placeholder="시작 날짜 및 시간"
                required
            />
            <small v-if="submitted && !advertisement.startDate" class="error-message">시작일시를 선택해주세요.</small>
          </div>

          <div class="form-field">
            <label for="endDate">종료일시</label>
            <PrimeCalendar
                id="endDate"
                v-model="advertisement.endDate"
                :class="{'p-invalid': submitted && !advertisement.endDate}"
                :minDate="advertisement.startDate"
                :showTime="true"
                dateFormat="yy-mm-dd"
                hourFormat="24"
                placeholder="종료 날짜 및 시간"
                required
            />
            <small v-if="submitted && !advertisement.endDate" class="error-message">종료일시를 선택해주세요.</small>
            <small
                v-if="advertisement.endDate && advertisement.startDate && advertisement.endDate <= advertisement.startDate"
                class="error-message">종료일시는 시작일시보다 이후여야 합니다.</small>
          </div>
        </div>
      </div>

      <!-- 표시 순서 및 활성화 상태 -->
      <div class="form-section">
        <div class="grid-layout">
          <div class="form-field">
            <label for="displayOrder">표시 순서</label>
            <PrimeInputNumber
                id="displayOrder"
                v-model="advertisement.displayOrder"
                :class="{'p-invalid': submitted && advertisement.displayOrder === null}"
                :min="0"
                class="full-width"
                mode="decimal"
                required
            />
            <small v-if="submitted && advertisement.displayOrder === null" class="error-message">표시 순서를 입력해주세요.</small>
            <small class="helper-text">낮은 숫자가 먼저 표시됩니다</small>
          </div>

          <div class="form-field toggle-field">
            <label>상태</label>
            <PrimeInputSwitch
                v-model="advertisement.isActive"
                :binary="true"
            />
            <span class="status-label">{{ advertisement.isActive ? '활성화됨' : '비활성화됨' }}</span>
          </div>
        </div>
      </div>

      <!-- 오류 메시지 표시 -->
      <div v-if="errorMessage" class="error-container">
        <i class="pi pi-exclamation-triangle error-icon"></i>
        <span>{{ errorMessage }}</span>
      </div>
    </form>

    <!-- 모달 하단 버튼 -->
    <template #footer>
      <div class="modal-footer">
        <PrimeButton
            class="p-button-text"
            label="취소"
            @click="hideDialog"
        />
        <PrimeButton
            :label="isEditing ? '수정 완료' : '저장'"
            :loading="isLoading"
            class="save-button"
            icon="pi pi-check"
            @click="saveAdvertisement"
        />
      </div>
    </template>
  </PrimeDialog>
</template>

<script>
// authService 또는 API 호출을 위한 서비스 임포트
import authService from '@/services/authService';
import {mapActions} from 'vuex';

export default {
  name: 'AdvertisementModal',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    editAdData: {
      type: Object,
      default: null
    }
  },
  emits: ['update:visible', 'saved'],
  data() {
    return {
      advertisement: this.getDefaultAd(),
      selectedFile: null,
      selectedFilePreviewUrl: null,
      currentFileUrl: null,
      isLoading: false,
      errorMessage: '',
      submitted: false,
      isDragging: false
    };
  },
  computed: {
    localVisible: {
      get() {
        return this.visible;
      },
      set(value) {
        this.$emit('update:visible', value);
      }
    },
    isEditing() {
      return !!this.editAdData && this.editAdData.id != null;
    }
  },
  methods: {
    ...mapActions(['fetchActiveAdvertisements']),

    getDefaultAd() {
      const now = new Date();
      const sevenDaysLater = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
      return {
        id: null,
        title: '',
        linkUrl: '',
        startDate: now,
        endDate: sevenDaysLater,
        displayOrder: 0,
        isActive: true,
      };
    },

    hideDialog() {
      this.localVisible = false;
    },

    onHide() {
      this.resetForm();
      this.$emit('update:visible', false);
    },

    resetForm() {
      this.advertisement = this.getDefaultAd();
      this.selectedFile = null;
      this.selectedFilePreviewUrl = null;
      this.currentFileUrl = null;
      this.isLoading = false;
      this.errorMessage = '';
      this.submitted = false;
      if (this.$refs.imageFileInput) {
        this.$refs.imageFileInput.value = '';
      }
    },

    // 파일 입력 관련 메서드
    triggerFileInput() {
      this.$refs.imageFileInput.click();
    },

    onDragOver() {
      this.isDragging = true;
    },

    onDragLeave() {
      this.isDragging = false;
    },

    onFileDrop(event) {
      this.isDragging = false;
      const file = event.dataTransfer.files[0];
      if (file && file.type.startsWith('image/')) {
        this.selectedFile = file;
        this.$refs.imageFileInput.files = event.dataTransfer.files;
        this.createFilePreview(file);
      }
    },

    handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        this.selectedFile = file;
        this.createFilePreview(file);
      } else {
        this.selectedFile = null;
        this.selectedFilePreviewUrl = null;
      }
    },

    createFilePreview(file) {
      if (file) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.selectedFilePreviewUrl = e.target.result;
        };
        reader.readAsDataURL(file);
      }
    },

    resetFileInput() {
      this.selectedFile = null;
      this.selectedFilePreviewUrl = null;
      if (this.$refs.imageFileInput) {
        this.$refs.imageFileInput.value = '';
      }
    },

    hasImage() {
      if (this.isEditing) {
        return !!this.currentFileUrl || !!this.selectedFile;
      }
      return !!this.selectedFile;
    },

    // 날짜 포맷 메서드
    formatDateToISO(date) {
      if (!date) return null;
      try {
        const tzoffset = date.getTimezoneOffset() * 60000;
        const localISOTime = new Date(date.getTime() - tzoffset).toISOString().slice(0, -1);
        return localISOTime.split('.')[0];
      } catch (e) {
        console.error("날짜 변환 오류:", e);
        return null;
      }
    },

    // 폼 유효성 검사
    validateForm() {
      this.submitted = true;
      let isValid = true;

      if (!this.isEditing && !this.selectedFile) {
        this.errorMessage = '광고 이미지를 선택해주세요.';
        isValid = false;
      }

      if (!this.advertisement.startDate || !this.advertisement.endDate || this.advertisement.displayOrder === null) {
        if (isValid) this.errorMessage = '필수 입력 항목(기간, 순서)을 확인해주세요.';
        isValid = false;
      }

      if (this.advertisement.endDate && this.advertisement.startDate && this.advertisement.endDate <= this.advertisement.startDate) {
        if (isValid) this.errorMessage = '종료일시는 시작일시보다 이후여야 합니다.';
        isValid = false;
      }

      if (isValid) {
        this.errorMessage = '';
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

      const formData = new FormData();

      const adData = {
        title: this.advertisement.title || '',
        linkUrl: this.advertisement.linkUrl || null,
        displayOrder: this.advertisement.displayOrder,
        isActive: this.advertisement.isActive,
        startDate: this.formatDateToISO(this.advertisement.startDate),
        endDate: this.formatDateToISO(this.advertisement.endDate),
      };

      formData.append('advertisement', new Blob([JSON.stringify(adData)], {type: 'application/json'}));

      if (this.selectedFile) {
        formData.append('imageFile', this.selectedFile, this.selectedFile.name);
      }

      try {
        let response;
        if (this.isEditing) {
          console.log(`광고 수정 API 호출 (ID: ${this.advertisement.id}). 페이로드:`, adData, "파일:", this.selectedFile ? this.selectedFile.name : "변경 없음");
          response = await authService.updateAdvertisement(this.advertisement.id, formData);
        } else {
          console.log('광고 생성 API 호출. 페이로드:', adData, "파일:", this.selectedFile ? this.selectedFile.name : "없음");
          response = await authService.createAdvertisementFormData(formData);
        }

        console.log("API 응답:", response);

        this.$emit('saved');
        this.hideDialog();
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
      this.advertisement = {
        ...this.getDefaultAd(),
        id: data.id,
        title: data.title || '',
        linkUrl: data.linkUrl,
        displayOrder: data.displayOrder,
        isActive: data.isActive,
        startDate: data.startDate ? new Date(data.startDate) : null,
        endDate: data.endDate ? new Date(data.endDate) : null,
      };
      this.currentFileUrl = data.fileUrl || null;
      this.selectedFile = null;
      this.selectedFilePreviewUrl = null;
      this.submitted = false;
      this.errorMessage = '';
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        if (this.isEditing) {
          this.populateFormForEdit(this.editAdData);
        } else {
          this.resetForm();
        }
      }
    },

    'advertisement.startDate'(newStartDate, oldStartDate) {
      if (oldStartDate && this.advertisement.endDate && newStartDate && this.advertisement.endDate < newStartDate) {
        this.advertisement.endDate = new Date(newStartDate.getTime());
      }
    }
  }
};
</script>

<style scoped>
.ad-modal {
  max-width: 600px;
  width: 100%;
}

.form-section {
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.form-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.form-field {
  margin-bottom: 1rem;
}

.form-field:last-child {
  margin-bottom: 0;
}

.form-field label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #333;
  font-size: 0.95rem;
}

.grid-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

@media (max-width: 768px) {
  .grid-layout {
    grid-template-columns: 1fr;
  }
}

.full-width {
  width: 100%;
}

.image-preview-container {
  margin-bottom: 1rem;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.image-preview {
  width: 100%;
  height: auto;
  max-height: 200px;
  object-fit: contain;
  background-color: #f8f8f8;
  display: block;
}

.image-caption {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 4px 8px;
  font-size: 0.8rem;
  text-align: center;
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #f8f8f8;
  font-size: 0.85rem;
  color: #555;
}

.upload-area {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 2rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #f9f9f9;
}

.upload-area:hover, .upload-area.dragging {
  border-color: #2196F3;
  background-color: #e3f2fd;
}

.upload-area.p-invalid {
  border-color: #e24c4c;
  background-color: #ffeded;
}

.upload-area.has-file {
  display: none;
}

.upload-icon {
  font-size: 2rem;
  color: #666;
  margin-bottom: 0.5rem;
  display: block;
}

.hidden-file-input {
  display: none;
}

.error-message {
  color: #e24c4c;
  font-size: 0.85rem;
  margin-top: 0.25rem;
  display: block;
}

.helper-text {
  color: #666;
  font-size: 0.85rem;
  margin-top: 0.25rem;
  display: block;
}

.toggle-field {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 0.5rem;
}

.status-label {
  margin-left: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.input-with-icon {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
}

.input-with-icon .p-inputtext {
  padding-left: 2.5rem;
}

.error-container {
  display: flex;
  align-items: center;
  background-color: #fff8f8;
  border-left: 4px solid #e24c4c;
  padding: 0.75rem;
  margin-top: 1rem;
  border-radius: 4px;
}

.error-icon {
  color: #e24c4c;
  margin-right: 0.5rem;
  font-size: 1.1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.save-button {
  min-width: 120px;
}

/* PrimeVue 컴포넌트 스타일 오버라이드 */
:deep(.p-calendar),
:deep(.p-inputnumber),
:deep(.p-inputtext) {
  width: 100%;
}

:deep(.p-inputswitch) {
  margin-right: 0.5rem;
}

:deep(.p-dialog-header) {
  padding: 1.25rem 1.5rem 0.75rem;
}

:deep(.p-dialog-content) {
  padding: 0 1.5rem 1.5rem;
}

:deep(.p-dialog-footer) {
  padding: 1rem 1.5rem;
  border-top: 1px solid #f0f0f0;
}

:deep(.p-button.save-button) {
  background-color: #2196F3;
  border-color: #2196F3;
}

:deep(.p-button.save-button:hover) {
  background-color: #0d8aee;
  border-color: #0d8aee;
}
</style>
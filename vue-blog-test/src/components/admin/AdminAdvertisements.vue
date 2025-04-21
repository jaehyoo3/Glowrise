<template>
  <div class="advertisement-management-tab">
    <AdvertisementModal
        v-model:visible="isAdModalVisible"
        :editAdData="selectedAdData"
        @saved="handleAdSaved"
    />

    <PrimeConfirmDialog group="adConfirm"></PrimeConfirmDialog>

    <PrimeDataTable
        :loading="isLoading"
        :paginator="advertisements.length > 10"
        :rowHover="true"
        :rows="10"
        :rowsPerPageOptions="[5, 10, 20]"
        :value="advertisements"
        class="p-datatable-sm ad-table"
        currentPageReportTemplate="{first}부터 {last}까지 / 총 {totalRecords}개"
        paginatorTemplate="CurrentPageReport FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown"
        responsiveLayout="scroll"
    >
      <template #header>
        <div class="table-header">
          <span class="p-input-icon-left">
              <i class="pi pi-search"/>
              </span>
          <PrimeButton
              class="p-button-success p-button-sm"
              icon="pi pi-plus"
              label="신규 광고 등록"
              @click="openCreateModal"
          />
        </div>
      </template>
      <template #empty>
        등록된 광고가 없습니다.
      </template>
      <template #loading>
        광고 목록을 불러오는 중입니다...
      </template>

      <PrimeColumn :sortable="true" field="id" header="ID" style="width: 5%"></PrimeColumn>
      <PrimeColumn header="이미지" style="width: 15%">
        <template #body="slotProps">
          <img
              v-if="slotProps.data.fileUrl"
              :alt="'광고 ' + slotProps.data.id"
              :src="getFullImageUrl(slotProps.data.fileUrl)"
              class="ad-thumbnail"
          />
          <span v-else>-</span>
        </template>
      </PrimeColumn>
      <PrimeColumn :sortable="true" field="title" header="제목" style="width: 15%">
        <template #body="slotProps">
          {{ slotProps.data.title || '(제목 없음)' }}
        </template>
      </PrimeColumn>
      <PrimeColumn field="linkUrl" header="링크 URL" style="width: 20%">
        <template #body="slotProps">
          <a v-if="slotProps.data.linkUrl" :href="slotProps.data.linkUrl" class="link-url" rel="noopener noreferrer"
             target="_blank">
            {{ slotProps.data.linkUrl }}
          </a>
          <span v-else>-</span>
        </template>
      </PrimeColumn>
      <PrimeColumn :sortable="true" field="startDate" header="시작일시" style="width: 10%">
        <template #body="slotProps">
          {{ formatDateTime(slotProps.data.startDate) }}
        </template>
      </PrimeColumn>
      <PrimeColumn :sortable="true" field="endDate" header="종료일시" style="width: 10%">
        <template #body="slotProps">
          {{ formatDateTime(slotProps.data.endDate) }}
        </template>
      </PrimeColumn>
      <PrimeColumn :sortable="true" field="displayOrder" header="순서" style="width: 5%"></PrimeColumn>
      <PrimeColumn :sortable="true" field="active" header="상태" style="width: 5%">
        <template #body="slotProps">
          <PrimeTag :severity="slotProps.data.active ? 'success' : 'danger'"
                    :value="slotProps.data.active ? '활성' : '비활성'"></PrimeTag>
        </template>
      </PrimeColumn>
      <PrimeColumn header="관리" style="width: 10%; text-align: center">
        <template #body="slotProps">
          <PrimeButton v-tooltip.top="'수정'" class="p-button-rounded p-button-text p-button-info p-mr-2"
                       icon="pi pi-pencil" @click="openEditModal(slotProps.data)"/>
          <PrimeButton v-tooltip.top="'삭제'" class="p-button-rounded p-button-text p-button-danger"
                       icon="pi pi-trash" @click="confirmDeleteAd(slotProps.data)"/>
        </template>
      </PrimeColumn>

    </PrimeDataTable>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useConfirm} from "primevue/useconfirm"; // ConfirmDialog 사용
import {useToast} from "primevue/usetoast"; // Toast 사용
import authService from '@/services/authService'; // API 서비스
import AdvertisementModal from './AdvertisementModal.vue'; // 기존 광고 생성/수정 모달

// PrimeVue 컴포넌트 자동 임포트 가정 (main.js 설정)
// 로컬 임포트 시: import PrimeDataTable from 'primevue/datatable'; import PrimeColumn from 'primevue/column'; import PrimeButton from 'primevue/button'; 등

const confirm = useConfirm();
const toast = useToast();

const advertisements = ref([]); // 광고 목록 데이터
const isLoading = ref(false); // 로딩 상태
const isAdModalVisible = ref(false); // 광고 생성/수정 모달 표시 여부
const selectedAdData = ref(null); // 수정할 광고 데이터

// API 기본 URL (이미지 경로 생성용)
const apiBaseUrl = computed(() => process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080');

// 광고 목록 불러오기
const fetchAdvertisements = async () => {
  isLoading.value = true;
  try {
    // authService에 모든 광고 목록을 가져오는 함수 필요 (getAdvertisementsForAdmin 등)
    advertisements.value = await authService.getAdvertisementsForAdmin(); // 관리자용 API 호출
    console.log("관리자 광고 목록:", advertisements.value);
  } catch (error) {
    console.error("관리자 광고 목록 로딩 실패:", error);
    toast.add({severity: 'error', summary: '오류', detail: '광고 목록을 불러오는데 실패했습니다.', life: 3000});
    advertisements.value = []; // 실패 시 빈 배열로 초기화
  } finally {
    isLoading.value = false;
  }
};

// 컴포넌트 마운트 시 목록 로드
onMounted(() => {
  fetchAdvertisements();
});

// 신규 광고 등록 모달 열기
const openCreateModal = () => {
  selectedAdData.value = null; // 수정 데이터 초기화
  isAdModalVisible.value = true;
};

// 광고 수정 모달 열기
const openEditModal = (adData) => {
  selectedAdData.value = {...adData}; // 수정할 데이터 복사하여 설정
  isAdModalVisible.value = true;
};

// 광고 저장/수정 완료 후 처리
const handleAdSaved = () => {
  isAdModalVisible.value = false; // 모달 닫기
  fetchAdvertisements(); // 목록 새로고침
};

// 광고 삭제 확인
const confirmDeleteAd = (adData) => {
  confirm.require({
    group: 'adConfirm', // ConfirmDialog의 group 속성과 일치
    message: `'${adData.title || 'ID: ' + adData.id}' 광고를 정말 삭제하시겠습니까? 연결된 이미지 파일도 함께 삭제됩니다.`,
    header: '광고 삭제 확인',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: '삭제',
    rejectLabel: '취소',
    acceptClass: 'p-button-danger',
    accept: async () => {
      await deleteAd(adData.id); // 삭제 실행
    },
    reject: () => {
      // 취소 시 동작 없음
    }
  });
};

// 광고 삭제 실행
const deleteAd = async (id) => {
  isLoading.value = true; // 로딩 표시 (선택 사항)
  try {
    await authService.deleteAdvertisement(id); // 삭제 API 호출
    toast.add({severity: 'success', summary: '삭제 완료', detail: '광고가 삭제되었습니다.', life: 3000});
    fetchAdvertisements(); // 목록 새로고침
  } catch (error) {
    console.error("광고 삭제 실패:", error);
    toast.add({severity: 'error', summary: '삭제 실패', detail: '광고 삭제 중 오류가 발생했습니다.', life: 3000});
  } finally {
    isLoading.value = false;
  }
};

// 날짜/시간 포맷팅 헬퍼
const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return '-';
  try {
    const date = new Date(dateTimeString);
    // 한국 시간 기준 년-월-일 시:분 형식
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false // 24시간 표기
    }).replace(/\. /g, '.').replace(/\.$/, ''); // 불필요한 . 제거
  } catch (e) {
    return dateTimeString; // 변환 실패 시 원본 반환
  }
};

// 이미지 전체 URL 생성 헬퍼
const getFullImageUrl = (relativeUrl) => {
  if (!relativeUrl) return '';
  if (relativeUrl.startsWith('http')) return relativeUrl;
  const baseUrl = apiBaseUrl.value.replace(/\/$/, '');
  const path = relativeUrl.replace(/^\//, '');
  return `${baseUrl}/${path}`;
};

</script>

<style scoped>
.advertisement-management-tab {
  /* 탭 내부 스타일 */
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ad-thumbnail {
  width: 100px;
  height: auto;
  max-height: 60px;
  object-fit: contain;
  border-radius: 4px;
  vertical-align: middle;
}

.link-url {
  color: var(--primary-color);
  text-decoration: none;
  word-break: break-all; /* 긴 URL 줄바꿈 */
}

.link-url:hover {
  text-decoration: underline;
}

/* DataTable 소형화 스타일 (필요시 추가) */
.p-datatable-sm .p-datatable-tbody > tr > td {
  padding: 0.5rem 0.75rem;
}

.p-datatable-sm .p-datatable-thead > tr > th {
  padding: 0.75rem;
}

/* 관리 버튼 간격 조정 */
:deep(.p-datatable-tbody td:last-child .p-button) {
  margin: 0 0.1rem;
}

</style>
<template>
  <div v-if="isVisible && activeAdvertisements && activeAdvertisements.length > 0"
       class="advertisement-container p-shadow-2">
    <Carousel
        v-if="isSlider"
        :autoplayInterval="5000"
        :circular="activeAdvertisements.length > 1"
        :numScroll="1"
        :numVisible="1"
        :responsiveOptions="responsiveOptions"
        :showIndicators="activeAdvertisements.length > 1"
        :showNavigators="activeAdvertisements.length > 1"
        :value="activeAdvertisements"
        class="ad-carousel"
    >
      <template #item="slotProps">
        <div class="ad-item">
          <img
              :alt="'Advertisement ' + slotProps.data.id"
              :src="slotProps.data.imageUrl"
              class="ad-image"
              style="cursor: pointer;"
              @click="handleImageClick(slotProps.data.targetUrl)"
          />
        </div>
      </template>
    </Carousel>

    <div v-else class="ad-item">
      <img
          :alt="'Advertisement ' + activeAdvertisements[0].id"
          :src="activeAdvertisements[0].imageUrl"
          class="ad-image"
          style="cursor: pointer;"
          @click="handleImageClick(activeAdvertisements[0].targetUrl)"
      />
    </div>

    <div class="ad-controls p-d-flex p-jc-between p-ai-center p-px-2 p-py-1">
      <div class="p-field-checkbox p-m-0">
        <Checkbox v-model="dontShowToday" :binary="true" inputId="dontShowTodayCheck"/>
        <label class="p-ml-1" for="dontShowTodayCheck">24시간동안 보지않기</label>
      </div>
      <Button
          aria-label="닫기"
          class="p-button-rounded p-button-text p-button-sm close-button"
          icon="pi pi-times"
          @click="closeAd"
      />
    </div>
  </div>
</template>

<script>
import {mapActions, mapState} from 'vuex'; // Vuex 헬퍼 함수 임포트

// PrimeVue 컴포넌트는 main.js에서 전역 등록했으므로 여기서는 임포트 불필요
// 만약 로컬 등록 시: import Carousel from 'primevue/carousel'; import Checkbox from 'primevue/checkbox'; import Button from 'primevue/button';

export default {
  name: 'MainAdvertisement',
  // components: { Carousel, Checkbox, Button }, // 로컬 등록 시 필요
  data() {
    return {
      isVisible: true, // 컴포넌트 표시 여부
      dontShowToday: false, // '24시간 보지 않기' 체크박스 모델
      hideAdStorageKey: 'hideAdUntilTimestamp', // localStorage 키 이름
      // Carousel 반응형 옵션 설정
      responsiveOptions: [
        {
          breakpoint: '1024px', // 화면 너비 1024px 이하
          numVisible: 1, // 보이는 아이템 수
          numScroll: 1 // 스크롤 시 이동할 아이템 수
        },
        {
          breakpoint: '600px', // 화면 너비 600px 이하
          numVisible: 1,
          numScroll: 1
        },
        {
          breakpoint: '480px', // 화면 너비 480px 이하
          numVisible: 1,
          numScroll: 1
        }
      ]
    };
  },
  computed: {
    // Vuex 스토어의 'activeAdvertisements' 상태를 컴포넌트의 computed 속성으로 매핑
    ...mapState(['activeAdvertisements']), // 모듈 사용 시: ...mapState('모듈명', ['activeAdvertisements'])

    // 광고가 2개 이상인지 여부 계산
    isSlider() {
      // activeAdvertisements가 존재하고 길이가 1보다 큰지 확인
      return this.activeAdvertisements && this.activeAdvertisements.length > 1;
    }
  },
  mounted() {
    // 컴포넌트가 마운트될 때 '숨김' 상태 확인
    this.checkHideStatus();
    // 숨김 상태가 아니면 Vuex 액션을 호출하여 광고 데이터 가져오기
    if (this.isVisible) {
      this.fetchActiveAdvertisements();
    }
  },
  methods: {
    // Vuex 스토어의 'fetchActiveAdvertisements' 액션을 컴포넌트 메소드로 매핑
    ...mapActions(['fetchActiveAdvertisements']), // 모듈 사용 시: ...mapActions('모듈명', ['fetchActiveAdvertisements'])

    // localStorage를 확인하여 광고를 숨겨야 하는지 체크하는 메소드
    checkHideStatus() {
      const hideUntil = localStorage.getItem(this.hideAdStorageKey);
      if (hideUntil) {
        const now = new Date().getTime();
        // 저장된 시간값이 현재 시간보다 미래이면 숨김 처리
        if (now < parseInt(hideUntil, 10)) {
          console.log('광고 숨김 기간입니다.');
          this.isVisible = false;
        } else {
          // 만료되었으면 localStorage에서 해당 키 제거
          console.log('광고 숨김 기간 만료.');
          localStorage.removeItem(this.hideAdStorageKey);
        }
      }
    },
    // 광고 닫기 메소드
    closeAd() {
      this.isVisible = false;
      // '24시간 보지 않기'가 체크된 상태에서 닫으면 localStorage에 만료 시간 저장
      if (this.dontShowToday) {
        const now = new Date();
        // 현재 시간 + 24시간 계산
        const hideUntilTimestamp = now.getTime() + (24 * 60 * 60 * 1000);
        localStorage.setItem(this.hideAdStorageKey, hideUntilTimestamp.toString());
        console.log(`광고를 ${new Date(hideUntilTimestamp).toLocaleString()}까지 숨깁니다.`);
        // 사용자에게 알림 표시 (Toast 사용 - App.vue 등에 Toast 컴포넌트 필요)
        // this.$toast.add({severity:'info', summary: '광고 숨김', detail: '24시간 동안 이 광고를 보지 않습니다.', life: 3000});
      }
    },
    // 광고 이미지 클릭 시 호출되는 메소드
    handleImageClick(url) {
      // URL이 유효하면 새 탭에서 열기
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
        // 필요하다면 여기에 클릭 통계 API 호출 로직 추가
      }
    }
    // 체크박스 v-model 사용으로 별도 change 핸들러 불필요
  },
  watch: {
    // Vuex 스토어의 광고 데이터가 변경될 때 감지 (선택 사항)
    activeAdvertisements(newVal, oldVal) {
      // 데이터 로드 후 광고가 없다면 컴포넌트 숨김 처리 등을 할 수 있음
      if (this.isVisible && (!newVal || newVal.length === 0)) {
        console.log("활성 광고가 없습니다.");
        // 상황에 따라 여기서 isVisible = false 처리 결정
      }
      // 데이터가 변경되었을 때 캐러셀 업데이트 관련 로직 필요 시 추가 (보통 자동 처리됨)
    }
  }
};
</script>

<style scoped>
/* 광고 컨테이너 기본 스타일 */
.advertisement-container {
  position: relative; /* 필요에 따라 fixed, absolute 등으로 변경 */
  width: 100%; /* 너비 조정 */
  max-width: 800px; /* 최대 너비 예시 */
  margin: 1rem auto; /* 상하 여백 및 가운데 정렬 예시 */
  overflow: hidden; /* 내부 요소 넘침 방지 */
  border-radius: 6px; /* PrimeVue 기본 테두리 둥글기 값 */
  background-color: #ffffff; /* 그림자 효과를 위해 배경색 지정 */
}

/* 캐러셀 내부 아이템 스타일 */
.ad-carousel .ad-item {
  text-align: center; /* 이미지 가운데 정렬 */
}

/* 캐러셀 및 단일 이미지 스타일 */
.ad-carousel .ad-image,
.advertisement-container > .ad-item > .ad-image {
  width: 100%; /* 너비 100% */
  height: auto; /* 높이 자동 (비율 유지) */
  max-height: 400px; /* 최대 높이 제한 예시 */
  object-fit: cover; /* 이미지가 영역을 비율 유지하며 채우도록 */
  display: block; /* 이미지 하단 여백 제거 */
}

/* 하단 컨트롤 바 스타일 */
.ad-controls {
  position: absolute; /* 컨테이너 하단에 절대 위치 */
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: rgba(0, 0, 0, 0.7); /* 반투명 검은색 배경 */
  color: #ffffff; /* 텍스트 색상 흰색 */
  box-sizing: border-box; /* 패딩 포함하여 너비 계산 */
  z-index: 10; /* 캐러셀 인디케이터보다 위에 오도록 */
}

/* 컨트롤 바 내부 라벨 스타일 */
.ad-controls label {
  color: #ffffff; /* 라벨 텍스트 색상 */
  vertical-align: middle; /* 세로 정렬 */
  cursor: pointer; /* 마우스 커서 포인터 */
  font-size: 0.875rem; /* 글꼴 크기 조정 */
}

/* 체크박스 정렬 개선 */
.p-field-checkbox {
  display: inline-flex; /* Flex 사용하여 내부 요소 정렬 */
  align-items: center; /* 세로 중앙 정렬 */
}

/* 닫기 버튼 스타일 */
.close-button {
  color: #ffffff !important; /* 버튼 텍스트 색상 강제 지정 */
  width: 2rem; /* 버튼 너비 */
  height: 2rem; /* 버튼 높이 */
}

/* 닫기 버튼 호버 효과 */
.close-button:hover, .close-button:focus {
  background-color: rgba(255, 255, 255, 0.1) !important; /* 살짝 밝아지는 배경 */
}

/* 캐러셀 네비게이션 버튼 및 인디케이터 위치 미세 조정 (필요시) */
/* :deep()은 scoped style 내에서 자식 컴포넌트 스타일을 수정할 때 사용 */
:deep(.p-carousel-prev),
:deep(.p-carousel-next) {
  /* 예: 버튼 위치 조정
  top: calc(50% - 1rem); */
}

/* 캐러셀 인디케이터 스타일 및 위치 조정 */
:deep(.p-carousel-indicators) {
  padding: 0; /* 기본 패딩 제거 */
  position: absolute; /* 절대 위치 */
  bottom: 45px; /* 컨트롤 바 높이만큼 위로 올림 (값 조정 필요) */
  left: 50%; /* 가로 중앙 */
  transform: translateX(-50%); /* 정확한 가로 중앙 정렬 */
  z-index: 5; /* 컨트롤 바 보다는 아래, 이미지 보다는 위 */
}

/* 인디케이터 기본 버튼 스타일 */
:deep(.p-carousel-indicator button) {
  background-color: rgba(255, 255, 255, 0.5); /* 반투명 흰색 */
}

/* 활성화된 인디케이터 버튼 스타일 */
:deep(.p-carousel-indicator.p-highlight button) {
  background-color: #ffffff; /* 흰색 */
}
</style>
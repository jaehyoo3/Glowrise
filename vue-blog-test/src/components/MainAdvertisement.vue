
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
              :src="getFullImageUrl(slotProps.data.fileUrl)" class="ad-image"
              style="cursor: pointer;"
              @click="handleImageClick(slotProps.data.linkUrl)"/>
        </div>
      </template>
    </Carousel>

    <div v-else class="ad-item">
      <img
          :alt="'Advertisement ' + activeAdvertisements[0].id"
          :src="getFullImageUrl(activeAdvertisements[0].fileUrl)" class="ad-image"
          style="cursor: pointer;"
          @click="handleImageClick(activeAdvertisements[0].linkUrl)"/>
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
import {mapActions, mapState} from 'vuex';

export default {
  name: 'MainAdvertisement',
  data() {
    return {
      isVisible: true,
      dontShowToday: false,
      hideAdStorageKey: 'hideAdUntilTimestamp',
      responsiveOptions: [
        {breakpoint: '1024px', numVisible: 1, numScroll: 1},
        {breakpoint: '600px', numVisible: 1, numScroll: 1},
        {breakpoint: '480px', numVisible: 1, numScroll: 1}
      ]
    };
  },
  computed: {
    ...mapState(['activeAdvertisements']),
    isSlider() {
      return this.activeAdvertisements && this.activeAdvertisements.length > 1;
    },
    // ===== 백엔드 API 기본 URL을 가져오는 computed 속성 추가 =====
    apiBaseUrl() {
      // VUE_APP_API_BASE_URL 환경 변수 사용, 없으면 기본값 'http://localhost:8080'
      return process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080';
    }
    // ===================================================
  },
  mounted() {
    this.checkHideStatus();
    if (this.isVisible) {
      this.fetchActiveAdvertisements();
    }
  },
  methods: {
    ...mapActions(['fetchActiveAdvertisements']),
    checkHideStatus() {
      const hideUntil = localStorage.getItem(this.hideAdStorageKey);
      if (hideUntil) {
        const now = new Date().getTime();
        if (now < parseInt(hideUntil, 10)) {
          console.log('광고 숨김 기간입니다.');
          this.isVisible = false;
        } else {
          console.log('광고 숨김 기간 만료.');
          localStorage.removeItem(this.hideAdStorageKey);
        }
      }
    },
    closeAd() {
      this.isVisible = false;
      if (this.dontShowToday) {
        const now = new Date();
        const hideUntilTimestamp = now.getTime() + (24 * 60 * 60 * 1000);
        localStorage.setItem(this.hideAdStorageKey, hideUntilTimestamp.toString());
        console.log(`광고를 ${new Date(hideUntilTimestamp).toLocaleString()}까지 숨깁니다.`);
      }
    },
    handleImageClick(url) {
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    },
    // ===== 상대 경로를 전체 URL로 변환하는 메소드 추가 =====
    getFullImageUrl(relativeUrl) {
      if (!relativeUrl) {
        return ''; // 또는 기본 이미지 경로
      }
      // 이미 http로 시작하는 절대 경로이면 그대로 반환
      if (relativeUrl.startsWith('http')) {
        return relativeUrl;
      }
      // 백엔드 기본 URL과 상대 경로 조합 (슬래시 중복 방지)
      const baseUrl = this.apiBaseUrl.replace(/\/$/, ''); // 끝 슬래시 제거
      const path = relativeUrl.replace(/^\//, '');    // 시작 슬래시 제거
      return `${baseUrl}/${path}`;
    }
    // ===================================================
  },
  watch: {
    activeAdvertisements(newVal) {
      if (this.isVisible && (!newVal || newVal.length === 0)) {
        console.log("활성 광고가 없습니다.");
      }
    }
  }
};
</script>

<style scoped>
/* 기존 스타일 유지 */
.advertisement-container {
  position: relative;
  width: 100%;
  max-width: 800px;
  margin: 1rem auto;
  overflow: hidden;
  border-radius: 6px;
  background-color: #ffffff;
}
.ad-carousel .ad-item {
  text-align: center;
}
.ad-carousel .ad-image,
.advertisement-container > .ad-item > .ad-image {
  width: 100%;
  height: auto;
  max-height: 400px;
  object-fit: cover;
  display: block;
}
.ad-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  box-sizing: border-box;
  z-index: 10;
}
.ad-controls label {
  color: #ffffff;
  vertical-align: middle;
  cursor: pointer;
  font-size: 0.875rem;
}
.p-field-checkbox {
  display: inline-flex;
  align-items: center;
}
.close-button {
  color: #ffffff !important;
  width: 2rem;
  height: 2rem;
}
.close-button:hover, .close-button:focus {
  background-color: rgba(255, 255, 255, 0.1) !important;
}
:deep(.p-carousel-prev),
:deep(.p-carousel-next) {
}
:deep(.p-carousel-indicators) {
  padding: 0;
  position: absolute;
  bottom: 45px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
}
:deep(.p-carousel-indicator button) {
  background-color: rgba(255, 255, 255, 0.5);
}
:deep(.p-carousel-indicator.p-highlight button) {
  background-color: #ffffff;
}
</style>
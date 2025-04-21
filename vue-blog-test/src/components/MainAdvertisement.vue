<template>
  <div v-if="isVisible && activeAdvertisements && activeAdvertisements.length > 0"
       ref="adContainer"
       :style="{ top: position.y + 'px', left: position.x + 'px' }"
       class="advertisement-container"
       @mousedown="startDrag"
       @touchstart="startDragTouch">

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
              :src="getFullImageUrl(slotProps.data.fileUrl)"
              class="ad-image"
              @click="handleImageClick(slotProps.data.linkUrl)"/>
        </div>
      </template>
    </Carousel>

    <div v-else class="ad-item">
      <img
          :alt="'Advertisement ' + activeAdvertisements[0].id"
          :src="getFullImageUrl(activeAdvertisements[0].fileUrl)"
          class="ad-image"
          @click="handleImageClick(activeAdvertisements[0].linkUrl)"/>
    </div>

    <div class="ad-controls">
      <div class="checkbox-container">
        <input id="dontShowTodayCheck" v-model="dontShowToday" class="custom-checkbox" type="checkbox">
        <label for="dontShowTodayCheck">24시간동안 보지않기</label>
      </div>
      <button
          class="close-button"
          @click="closeAd">
        ✕
      </button>
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
      ],
      // 드래그 관련 상태
      isDragging: false,
      position: {
        x: 50,  // 초기 위치 - 화면 왼쪽에서 50px
        y: 70   // 초기 위치 - 화면 상단에서 20px
      },
      dragOffset: {
        x: 0,
        y: 0
      }
    };
  },
  computed: {
    ...mapState(['activeAdvertisements']),
    isSlider() {
      return this.activeAdvertisements && this.activeAdvertisements.length > 1;
    },
    apiBaseUrl() {
      return process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080';
    }
  },
  mounted() {
    this.checkHideStatus();
    if (this.isVisible) {
      this.fetchActiveAdvertisements();
      // 저장된 위치가 있으면 복원
      this.$nextTick(() => {
        this.loadSavedPosition();
      });
    }

    // 드래그 이벤트 리스너 추가
    document.addEventListener('mousemove', this.onDrag);
    document.addEventListener('mouseup', this.stopDrag);
    document.addEventListener('touchmove', this.onDragTouch, {passive: false});
    document.addEventListener('touchend', this.stopDragTouch);
  },
  beforeUnmount() {
    // 컴포넌트 제거 시 이벤트 리스너 정리
    document.removeEventListener('mousemove', this.onDrag);
    document.removeEventListener('mouseup', this.stopDrag);
    document.removeEventListener('touchmove', this.onDragTouch);
    document.removeEventListener('touchend', this.stopDragTouch);
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
      if (this.dontShowToday) {
        const now = new Date();
        const hideUntilTimestamp = now.getTime() + (24 * 60 * 60 * 1000);
        localStorage.setItem(this.hideAdStorageKey, hideUntilTimestamp.toString());
        console.log(`광고를 ${new Date(hideUntilTimestamp).toLocaleString()}까지 숨깁니다.`);
      }
      this.isVisible = false;
    },
    handleImageClick(url) {
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    },
    getFullImageUrl(relativeUrl) {
      if (!relativeUrl) {
        return '';
      }
      if (relativeUrl.startsWith('http')) {
        return relativeUrl;
      }
      const baseUrl = this.apiBaseUrl.replace(/\/$/, '');
      const path = relativeUrl.replace(/^\//, '');
      return `${baseUrl}/${path}`;
    },

    // 드래그 관련 메소드
    startDrag(event) {
      // 이미지 클릭 시에는 드래그 시작하지 않음
      if (event.target.tagName === 'IMG' || event.target.closest('.close-button') || event.target.closest('.checkbox-container')) {
        return;
      }

      this.isDragging = true;
      const rect = this.$refs.adContainer.getBoundingClientRect();
      this.dragOffset = {
        x: event.clientX - rect.left,
        y: event.clientY - rect.top
      };
      event.preventDefault();
    },

    onDrag(event) {
      if (!this.isDragging) return;

      const newX = event.clientX - this.dragOffset.x;
      const newY = event.clientY - this.dragOffset.y;

      // 화면 경계 체크
      const containerWidth = this.$refs.adContainer.offsetWidth;
      const containerHeight = this.$refs.adContainer.offsetHeight;

      // 화면 밖으로 나가지 않도록 제한
      const maxX = window.innerWidth - containerWidth;
      const maxY = window.innerHeight - containerHeight;

      this.position = {
        x: Math.max(0, Math.min(newX, maxX)),
        y: Math.max(0, Math.min(newY, maxY))
      };

      // 위치 저장
      this.savePosition();
    },

    stopDrag() {
      this.isDragging = false;
    },

    // 터치 이벤트 처리 (모바일 대응)
    startDragTouch(event) {
      // 이미지나 버튼 터치 시에는 드래그 시작하지 않음
      if (event.target.tagName === 'IMG' || event.target.closest('.close-button') || event.target.closest('.checkbox-container')) {
        return;
      }

      this.isDragging = true;
      const touch = event.touches[0];
      const rect = this.$refs.adContainer.getBoundingClientRect();
      this.dragOffset = {
        x: touch.clientX - rect.left,
        y: touch.clientY - rect.top
      };
    },

    onDragTouch(event) {
      if (!this.isDragging) return;

      // 스크롤 방지
      event.preventDefault();

      const touch = event.touches[0];
      const newX = touch.clientX - this.dragOffset.x;
      const newY = touch.clientY - this.dragOffset.y;

      // 화면 경계 체크
      const containerWidth = this.$refs.adContainer.offsetWidth;
      const containerHeight = this.$refs.adContainer.offsetHeight;

      // 화면 밖으로 나가지 않도록 제한
      const maxX = window.innerWidth - containerWidth;
      const maxY = window.innerHeight - containerHeight;

      this.position = {
        x: Math.max(0, Math.min(newX, maxX)),
        y: Math.max(0, Math.min(newY, maxY))
      };

      // 위치 저장
      this.savePosition();
    },

    stopDragTouch() {
      this.isDragging = false;
    },

    // 위치 저장 및 복원
    savePosition() {
      localStorage.setItem('adPosition', JSON.stringify(this.position));
    },

    loadSavedPosition() {
      const savedPosition = localStorage.getItem('adPosition');
      if (savedPosition && this.$refs.adContainer) {
        try {
          const position = JSON.parse(savedPosition);
          // 화면 크기 변경으로 위치가 화면 밖으로 나가지 않도록 체크
          const containerWidth = this.$refs.adContainer.offsetWidth;
          const containerHeight = this.$refs.adContainer.offsetHeight;
          const maxX = window.innerWidth - containerWidth;
          const maxY = window.innerHeight - containerHeight;

          this.position = {
            x: Math.max(0, Math.min(position.x, maxX)),
            y: Math.max(0, Math.min(position.y, maxY))
          };
        } catch (e) {
          console.error('광고 위치 복원 오류:', e);
        }
      }
    }
  },
  watch: {
    dontShowToday(newVal) {
      console.log('체크박스 상태 변경:', newVal);
    },
    activeAdvertisements(newVal) {
      if (this.isVisible && (!newVal || newVal.length === 0)) {
        console.log("활성 광고가 없습니다.");
      }
    }
  }
};
</script>

<style scoped>
.advertisement-container {
  position: fixed;
  /* width: 303px; /* 기존 */
  width: 303px !important; /* !important 추가하여 우선순위 높임 */
  overflow: hidden;
  /* background-color: yellow !important; /* 임시 배경색 제거 */
  /* border: 2px solid blue !important; /* 임시 테두리 제거 */
  background-color: #ffffff; /* 원래 배경색 */
  border: none; /* 원래 테두리 없음 */
  z-index: 1000;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  cursor: move;
  user-select: none;
  border-radius: 0px;
}

.ad-carousel .ad-item,
.advertisement-container > .ad-item {
  text-align: center;
  height: 215px; /* 참조 이미지 높이와 일치 */
  overflow: hidden;
  width: 303px; /* 컨테이너 너비 고정 */
}


.ad-carousel .ad-image,
.advertisement-container > .ad-item > .ad-image {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 또는 cover */
  display: block;
  cursor: pointer;
}
.ad-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px; /* 좌우 패딩 */
  background-color: rgba(0, 0, 0, 0.8);
  color: #ffffff;
  height: 22px;
  box-sizing: border-box;
  width: 100%;
}


.checkbox-container {
  display: flex;
  align-items: center;
}

.checkbox-container label {
  color: #ffffff;
  font-size: 11px;
  margin-left: 4px;
  cursor: pointer;
}

/* 기본 체크박스 대신 커스텀 체크박스 사용 */
.custom-checkbox {
  width: 12px;
  height: 12px;
  margin: 0;
  vertical-align: middle;
  appearance: auto;
  cursor: pointer;
}

.close-button {
  color: #ffffff;
  background: transparent;
  border: none;
  font-size: 14px;
  line-height: 1;
  padding: 2px 6px;
  cursor: pointer;
  outline: none;
}

.close-button:hover {
  opacity: 0.8;
}

/* 캐러셀 인디케이터 위치 조정 */
:deep(.p-carousel-indicators) {
  padding: 0;
  position: absolute;
  bottom: 26px; /* 컨트롤바 위에 위치 */
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
}

:deep(.p-carousel-indicator button) {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.5);
  margin: 0 3px;
}

:deep(.p-carousel-indicator.p-highlight button) {
  background-color: #ffffff;
}
</style>
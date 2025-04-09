<template>
  <NavBar/>
  <router-view/>
</template>

<script setup>
// NavBar 컴포넌트를 가져옵니다.
// @는 src 디렉토리를 가리키는 별칭입니다 (vue.config.js 또는 vite.config.js 설정).
import NavBar from '@/components/NavBar.vue';

// <script setup>을 사용하면 components 옵션에 등록할 필요 없이
// 템플릿에서 바로 NavBar를 사용할 수 있습니다.
</script>

<style>
/* 기존 스타일 유지 */
body {
  margin: 0;
  font-family: 'Noto Sans KR', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f5f6f7; /* 기본 배경색 설정 */
}

/* 선택 사항: NavBar가 fixed 또는 sticky일 경우 body 상단에 패딩을 추가하여
   컨텐츠가 NavBar 뒤에 가려지지 않도록 할 수 있습니다.
   NavBar의 실제 높이에 맞게 값을 조정해야 합니다. */
/*
body {
  padding-top: 70px; / 예시: NavBar 높이가 70px일 경우 /
}
*/
</style>
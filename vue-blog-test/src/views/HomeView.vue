<template>
  <div class="home">
    <NavBar />
    <div class="container mt-4">
      <h1>환영합니다!</h1>
      <p>블로그 플랫폼에 오신 것을 환영합니다.</p>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue'

export default {
  name: 'HomeView',
  components: {
    NavBar
  }
}
</script>
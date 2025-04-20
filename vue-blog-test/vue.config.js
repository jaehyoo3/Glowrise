const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,

  // 개발 서버 설정
  devServer: {
    port: 3000, // 프론트엔드 실행 포트

    // ===== 프록시 설정 추가 =====
    proxy: {
      '/api': { // '/api'로 시작하는 요청은
        target: 'http://localhost:8080', // 실제 백엔드 서버 주소로 전달
        changeOrigin: true,
      }
    }
    // =========================
  },

  chainWebpack: config => {
    config.plugin('define').tap(args => {
      args[0]['__VUE_OPTIONS_API__'] = JSON.stringify(true);
      args[0]['__VUE_PROD_DEVTOOLS__'] = JSON.stringify(false);
      args[0]['__VUE_PROD_HYDRATION_MISMATCH_DETAILS__'] = JSON.stringify(false);
      return args;
    });
  }
})
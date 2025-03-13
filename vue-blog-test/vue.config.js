const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 3000 // 원하는 포트 번호로 변경
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

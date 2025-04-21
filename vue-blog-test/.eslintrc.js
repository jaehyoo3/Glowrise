module.exports = {
    root: true,
    env: {
        node: true,
        'vue/setup-compiler-macros': true // defineProps/defineEmits 인식 설정
    },
    extends: [
        'plugin:vue/vue3-essential', // Vue 3 필수 규칙 적용
        'eslint:recommended'         // ESLint 추천 규칙 적용
    ],
    parserOptions: {
        ecmaVersion: 2020 // 최신 ECMAScript 기능 파싱 허용
    },
    rules: {
        // 필요에 따른 규칙 추가/수정 (예시)
        'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
        'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
        'vue/multi-word-component-names': 'off' // 컴포넌트 이름 규칙 완화
    }
};
<template>
  <div class="search-results-page container"><h1><span class="query-highlight">'{{ currentQuery }}'</span> 검색 결과</h1>

    <div v-if="loading" class="status-message loading">
      <p>결과를 불러오는 중입니다...</p>
    </div>

    <div v-else-if="error" class="status-message error">
      <p>검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>
      <p v-if="error.message" class="error-detail">{{ error.message }}</p>
    </div>

    <div v-else-if="!hasResults" class="status-message no-results">
      <p>검색 결과가 없습니다.</p>
    </div>

    <div v-else class="results-container">
      <section class="results-section posts-section">
        <h2>게시글 <span class="count">({{ postPageInfo.totalElements }}건)</span></h2>
        <ul v-if="results.posts?.content?.length" class="result-list">
          <li v-for="post in results.posts.content" :key="post.id" class="result-item post-item">
            <router-link :to="{ name: 'postDetail', params: { id: post.id } }" class="item-link">
              <h3 class="item-title">{{ post.title }}</h3>
            </router-link>
            <p class="item-snippet">{{ post.contentSnippet }}</p>
            <div class="item-meta">
              <span class="author-info">작성자: {{ post.user?.nickname || post.user?.email || '알 수 없음' }}</span>
              <span class="date-info">작성일: {{ formatDate(post.createdAt) }}</span>
            </div>
          </li>
        </ul>
        <p v-else class="no-section-results">일치하는 게시글이 없습니다.</p>
        <div v-if="postPageInfo.totalPages > 1" class="pagination">
          <button :disabled="postPageInfo.first" @click="goToPage(postPageInfo.number - 1)">이전</button>
          <span>페이지 {{ postPageInfo.number + 1 }} / {{ postPageInfo.totalPages }}</span>
          <button :disabled="postPageInfo.last" @click="goToPage(postPageInfo.number + 1)">다음</button>
        </div>
      </section>

      <section class="results-section users-section">
        <h2>계정 <span class="count">({{ userPageInfo.totalElements }}건)</span></h2>
        <ul v-if="results.users?.content?.length" class="result-list">
          <li v-for="user in results.users.content" :key="user.id" class="result-item user-item">
            <p class="user-info">
              <span class="user-nickname">{{ user.nickname }}</span>
              (<span class="user-email">{{ user.email }}</span>)
            </p>
          </li>
        </ul>
        <p v-else class="no-section-results">일치하는 계정이 없습니다.</p>
        <div v-if="userPageInfo.totalPages > 1" class="pagination">
          <button :disabled="userPageInfo.first" @click="goToPage(userPageInfo.number - 1)">이전</button>
          <span>페이지 {{ userPageInfo.number + 1 }} / {{ userPageInfo.totalPages }}</span>
          <button :disabled="userPageInfo.last" @click="goToPage(userPageInfo.number + 1)">다음</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
// useRoute 임포트 제거
import {useRouter} from 'vue-router';
import {searchIntegrated} from '@/services/searchService'; // API 서비스 임포트 경로 확인

// useRoute() 호출 제거
// const route = useRoute();
const router = useRouter();

// 컴포넌트 props 정의 (라우터에서 전달받음)
// defineProps는 <script setup> 내에서 자동으로 사용 가능 (import 불필요)
// eslint-disable-next-line no-undef
const props = defineProps({
  query: {
    type: String,
    required: true
  },
  initialPage: {
    type: Number,
    default: 0
  }
});

// --- 나머지 반응형 상태 변수 및 함수 정의는 이전과 동일 ---
const results = ref({posts: null, users: null});
const postPageInfo = ref({number: 0, totalPages: 0, totalElements: 0, first: true, last: true});
const userPageInfo = ref({number: 0, totalPages: 0, totalElements: 0, first: true, last: true});
const loading = ref(false);
const error = ref(null);
const currentQuery = ref('');
const currentPage = ref(0);
const pageSize = ref(10);

const hasResults = computed(() => {
  const postsExist = results.value.posts?.content?.length > 0;
  const usersExist = results.value.users?.content?.length > 0;
  return postsExist || usersExist;
});

const fetchResults = async (queryToFetch, pageToFetch) => {
  // ... (이전과 동일한 fetchResults 함수 내용) ...
  if (!queryToFetch) {
    results.value = {posts: null, users: null};
    postPageInfo.value = {number: 0, totalPages: 0, totalElements: 0, first: true, last: true};
    userPageInfo.value = {number: 0, totalPages: 0, totalElements: 0, first: true, last: true};
    currentQuery.value = '';
    return;
  }
  loading.value = true;
  error.value = null;
  currentQuery.value = queryToFetch;
  currentPage.value = pageToFetch;

  try {
    const data = await searchIntegrated(queryToFetch, pageToFetch, pageSize.value);
    results.value = data;

    if (data.posts) {
      postPageInfo.value = extractPageInfo(data.posts);
    }
    if (data.users) {
      userPageInfo.value = extractPageInfo(data.users);
    }
  } catch (err) {
    console.error("검색 결과 로딩 실패:", err);
    error.value = err;
    results.value = {posts: null, users: null};
  } finally {
    loading.value = false;
  }
};

const extractPageInfo = (pageData) => {
  // ... (이전과 동일한 extractPageInfo 함수 내용) ...
  return {
    number: pageData.number,
    totalPages: pageData.totalPages,
    totalElements: pageData.totalElements,
    first: pageData.first,
    last: pageData.last
  };
};

const goToPage = (page) => {
  // ... (이전과 동일한 goToPage 함수 내용) ...
  if (page >= 0 && currentQuery.value) {
    router.push({name: 'search', query: {q: currentQuery.value, page: page}});
  }
};

const formatDate = (dateString) => {
  // ... (이전과 동일한 formatDate 함수 내용) ...
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  } catch (e) {
    console.error("날짜 포맷 오류:", e);
    return dateString;
  }
};

// 컴포넌트 마운트 시 초기 검색 실행
onMounted(() => {
  // props에서 값을 읽어옴
  fetchResults(props.query, props.initialPage);
});

// 라우트 props(query 또는 initialPage)가 변경될 때마다 검색 다시 실행
watch(
    () => [props.query, props.initialPage],
    ([newQuery, newPage]) => {
      fetchResults(newQuery, newPage);
    }
    // immediate 옵션은 onMounted에서 초기 로딩을 하므로 제거해도 됨
);

</script>

<style scoped>
.container { /* 프로젝트의 기본 컨테이너 스타일 적용 */
  max-width: 1200px;
  margin: 2rem auto;
  padding: 0 1rem;
}

h1 {
  margin-bottom: 1.5rem;
  font-weight: 600;
}

.query-highlight {
  color: #007bff; /* 검색어 강조 색상 */
}

.status-message {
  text-align: center;
  padding: 2rem;
  color: #666;
  font-size: 1.1rem;
}

.status-message.error {
  color: #dc3545;
}

.error-detail {
  font-size: 0.9em;
  color: #888;
  margin-top: 0.5rem;
}

.results-container {
  display: flex;
  flex-direction: column; /* 기본 세로 배치, 화면 크면 가로 배치하도록 수정 가능 */
  gap: 2rem; /* 섹션 간 간격 */
}

/* 반응형: 화면 너비가 충분할 때 가로 배치 */
@media (min-width: 768px) {
  .results-container {
    flex-direction: row;
  }

  .results-section {
    flex: 1; /* 가로 배치 시 동일한 너비 차지 */
  }
}

.results-section {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1.5rem;
  background-color: #fff;
}

.results-section h2 {
  font-size: 1.4rem;
  margin-top: 0;
  margin-bottom: 1rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid #eee;
  font-weight: 500;
}

.results-section h2 .count {
  font-size: 0.9em;
  color: #555;
  font-weight: normal;
}

.result-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.result-item {
  padding: 1rem 0;
  border-bottom: 1px dashed #eee;
}

.result-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.result-item .item-link {
  text-decoration: none;
  color: inherit;
}

.result-item .item-link:hover .item-title {
  color: #0056b3; /* 호버 시 제목 색상 변경 */
}

.item-title {
  font-size: 1.15rem;
  margin-top: 0;
  margin-bottom: 0.4rem;
  font-weight: 600;
  color: #333;
}

.item-snippet, .user-info {
  font-size: 0.95rem;
  color: #555;
  line-height: 1.5;
  margin-bottom: 0.5rem;
}

.item-meta {
  font-size: 0.85rem;
  color: #777;
}

.item-meta span {
  margin-right: 1rem;
}

.user-nickname {
  font-weight: 500;
}

.user-email {
  color: #666;
}


.no-section-results {
  color: #777;
  padding: 1rem 0;
}

.pagination {
  margin-top: 1.5rem;
  text-align: center;
}

.pagination button {
  margin: 0 0.3rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  border: 1px solid #ddd;
  background-color: #fff;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.pagination button:hover:not(:disabled) {
  background-color: #f0f0f0;
}

.pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.pagination span {
  margin: 0 0.7rem;
  color: #555;
  font-size: 0.95rem;
}
</style>
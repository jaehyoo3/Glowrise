<template>
  <div class="search-results-page container">
    <h1>검색 결과: <span class="query-highlight">'{{ currentQuery }}'</span></h1>

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
        <div class="section-header">
          <h2>게시글</h2>
          <span class="count">{{ postPageInfo.totalElements }}건</span>
        </div>

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
          <button :disabled="postPageInfo.first" class="pagination-btn prev" @click="goToPage(postPageInfo.number - 1)">
            이전
          </button>
          <span>페이지 {{ postPageInfo.number + 1 }} / {{ postPageInfo.totalPages }}</span>
          <button :disabled="postPageInfo.last" class="pagination-btn next" @click="goToPage(postPageInfo.number + 1)">
            다음
          </button>
        </div>
      </section>

      <section class="results-section users-section">
        <div class="section-header">
          <h2>계정</h2>
          <span class="count">{{ userPageInfo.totalElements }}건</span>
        </div>

        <ul v-if="results.users?.content?.length" class="result-list">
          <li v-for="user in results.users.content" :key="user.id" class="result-item user-item">
            <div class="user-info">
              <span class="user-nickname">{{ user.nickname }}</span>
              <span class="user-email">{{ user.email }}</span>
            </div>
          </li>
        </ul>
        <p v-else class="no-section-results">일치하는 계정이 없습니다.</p>

        <div v-if="userPageInfo.totalPages > 1" class="pagination">
          <button :disabled="userPageInfo.first" class="pagination-btn prev" @click="goToPage(userPageInfo.number - 1)">
            이전
          </button>
          <span>페이지 {{ userPageInfo.number + 1 }} / {{ userPageInfo.totalPages }}</span>
          <button :disabled="userPageInfo.last" class="pagination-btn next" @click="goToPage(userPageInfo.number + 1)">
            다음
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import {searchIntegrated} from '@/services/searchService';

const router = useRouter();

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
  return {
    number: pageData.number,
    totalPages: pageData.totalPages,
    totalElements: pageData.totalElements,
    first: pageData.first,
    last: pageData.last
  };
};

const goToPage = (page) => {
  if (page >= 0 && currentQuery.value) {
    router.push({name: 'search', query: {q: currentQuery.value, page: page}});
  }
};

const formatDate = (dateString) => {
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

onMounted(() => {
  fetchResults(props.query, props.initialPage);
});

watch(
    () => [props.query, props.initialPage],
    ([newQuery, newPage]) => {
      fetchResults(newQuery, newPage);
    }
);
</script>

<style scoped>
/* 기본 레이아웃 */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
  font-family: 'Segoe UI', 'Roboto', 'Helvetica Neue', sans-serif;
}

/* 헤더 스타일 */
h1 {
  font-size: 28px;
  font-weight: 500;
  margin-bottom: 24px;
  color: #333;
  letter-spacing: -0.5px;
}

.query-highlight {
  color: #555;
  font-weight: 600;
}

/* 상태 메시지 스타일 */
.status-message {
  text-align: center;
  padding: 48px 0;
  background-color: #f9f9f9;
  border-radius: 4px;
  margin: 24px 0;
}

.status-message p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.status-message.error {
  background-color: #fff0f0;
}

.error-detail {
  font-size: 14px;
  color: #888;
  margin-top: 8px;
}

/* 결과 컨테이너 */
.results-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* 결과 섹션 스타일 */
.results-section {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.section-header h2 {
  font-size: 20px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.count {
  background-color: #f2f2f2;
  color: #666;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

/* 결과 목록 */
.result-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.result-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.15s ease;
}

.result-item:hover {
  background-color: #fafafa;
}

.result-item:last-child {
  border-bottom: none;
}

.item-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.item-title {
  font-size: 18px;
  font-weight: 500;
  margin: 0 0 8px 0;
  color: #333;
  transition: color 0.2s ease;
}

.item-link:hover .item-title {
  color: #555;
}

.item-snippet {
  font-size: 15px;
  color: #555;
  line-height: 1.5;
  margin: 0 0 12px 0;
}

.item-meta {
  display: flex;
  font-size: 13px;
  color: #777;
}

.item-meta span {
  margin-right: 16px;
}

/* 유저 정보 스타일 */
.user-item {
  padding: 12px 0;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-nickname {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-email {
  font-size: 14px;
  color: #777;
}

/* 페이지네이션 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.pagination-btn {
  background-color: #fff;
  border: 1px solid #ddd;
  color: #555;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  background-color: #f5f5f5;
  border-color: #ccc;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination span {
  margin: 0 16px;
  color: #666;
  font-size: 14px;
}

/* 결과 없음 */
.no-section-results {
  padding: 24px 0;
  text-align: center;
  color: #777;
  font-size: 15px;
}

/* 반응형 */
@media (max-width: 768px) {
  .results-container {
    grid-template-columns: 1fr;
  }

  .container {
    padding: 16px;
  }

  h1 {
    font-size: 22px;
  }
}
</style>
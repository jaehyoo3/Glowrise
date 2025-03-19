<template>
  <div class="post-detail container mt-4">
    <NavBar/>
    <h1>게시글 상세</h1>
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="post">
      <div class="post-content">
        <h2>{{ post.title }}</h2>
        <p>{{ post.content }}</p>
        <div v-if="post.fileIds && post.fileIds.length > 0">
          <p>첨부 파일: {{ post.fileIds.length }}개</p>
        </div>
        <div class="form-group">
          <label for="menuSelect">현재 메뉴</label>
          <select v-model="post.menuId" class="form-control" id="menuSelect" disabled>
            <option v-for="menu in allMenus" :key="menu.id" :value="menu.id" :disabled="isParentMenu(menu.id)">
              {{ menu.name }} {{ menu.parentId ? `(하위: ${getParentName(menu.parentId)})` : '' }}
            </option>
          </select>
        </div>
        <router-link :to="`/${blogUrl}/${menuId}/edit/${post.id}`" class="btn btn-primary mt-2">수정</router-link>
        <router-link :to="`/${blogUrl}/${menuId}`" class="btn btn-secondary mt-2 ml-2">목록으로</router-link>
      </div>
    </div>
    <div v-else>
      <h1>게시글을 찾을 수 없습니다.</h1>
      <router-link :to="`/`" class="btn btn-primary">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'PostDetail',
  components: {NavBar},
  props: {
    blogUrl: String,
    menuId: String,
    postId: String,
  },
  data() {
    return {
      post: null,
      allMenus: [],
      isLoading: true,
    };
  },
  watch: {
    '$route.query.refresh': {
      immediate: true,
      handler() {
        this.loadPostAndMenus();
      }
    }
  },
  async created() {
    console.log('PostDetail created with route params:', this.$route.params);
    console.log('Props received - blogUrl:', this.blogUrl, 'menuId:', this.menuId, 'postId:', this.postId);
    await this.loadPostAndMenus();
  },
  methods: {
    async loadPostAndMenus() {
      try {
        this.isLoading = true;
        console.log('Starting to fetch post with blogUrl:', this.blogUrl, 'menuId:', this.menuId, 'postId:', this.postId);

        // 게시글 데이터 가져오기
        const postResponse = await authService.getPostById(this.postId);
        console.log('Raw post response from authService:', JSON.stringify(postResponse));
        this.post = postResponse;

        // menuId 처리
        if (!this.post.menuId) {
          console.warn('post.menuId is null, using URL menuId as fallback:', this.menuId);
          this.post.menuId = Number(this.menuId); // 백엔드에서 null일 경우 URL 값 사용
        } else if (this.post.menuId !== Number(this.menuId)) {
          console.warn('menuId mismatch! Post:', this.post.menuId, 'URL:', this.menuId);
          // 백엔드 데이터를 우선 사용하되, 불일치 경고 출력
        }
        console.log('Assigned post:', JSON.stringify(this.post));

        // 블로그 데이터 가져오기
        const blogResponse = await authService.getBlogByUrl(this.blogUrl);
        console.log('Loaded blog:', JSON.stringify(blogResponse));
        this.blog = blogResponse;

        // 메뉴 데이터 가져오기
        const menusResponse = await authService.getMenusByBlogId(this.blog.id);
        console.log('Raw menus from authService:', JSON.stringify(menusResponse));
        this.allMenus = menusResponse;

        // 메뉴 계층 구조 처리 (필요 시)
        this.processMenus();

        // 메뉴 ID 유효성 확인
        const menuIds = this.allMenus.map(menu => menu.id);
        console.log('Checking menu match - post.menuId:', this.post.menuId, 'Available menu IDs:', menuIds);
        if (!menuIds.includes(this.post.menuId)) {
          console.warn('post.menuId does not match any available menu IDs');
          // 필요 시 추가 로직 (예: 기본 메뉴로 리다이렉트)
        }

        console.log('Finished loading, isLoading:', this.isLoading, 'Post:', JSON.stringify(this.post));
      } catch (error) {
        console.error('게시글 또는 메뉴 로드 실패:', error.message);
        this.post = null;
        this.blog = null;
        this.allMenus = [];
        this.$router.push('/login'); // 오류 시 로그인 페이지로 이동
      } finally {
        this.isLoading = false;
      }
    },

// 메뉴 계층 구조 처리 메서드 (필요 시)
    processMenus() {
      const menuMap = new Map();
      this.allMenus.forEach(menu => {
        menu.subMenus = []; // 하위 메뉴 배열 초기화
        menuMap.set(menu.id, menu);
      });

      this.allMenus.forEach(menu => {
        if (menu.parentId) {
          const parent = menuMap.get(menu.parentId);
          if (parent) {
            parent.subMenus.push(menu);
          }
        }
      });

      this.menus = this.allMenus.filter(menu => !menu.parentId); // 최상위 메뉴만 추출
      console.log('Processed menus:', JSON.stringify(this.menus));
    },
    getParentName(parentId) {
      const parent = this.allMenus.find(menu => menu.id === parentId);
      return parent ? parent.name : '';
    },
    isParentMenu(menuId) {
      return this.allMenus.some(menu => menu.parentId === menuId);
    },
  },
};
</script>

<style scoped>
.post-detail {
  max-width: 800px;
  margin: 0 auto;
}

.post-content {
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-control {
  width: 100%;
}

.btn {
  margin-right: 10px;
}

.ml-2 {
  margin-left: 10px;
}

#menuSelect option:disabled {
  color: #888;
  font-style: italic;
}
</style>
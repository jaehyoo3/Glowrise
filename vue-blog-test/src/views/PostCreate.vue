<template>
  <div class="post-create container mt-4">
    <NavBar/>
    <h1>새 게시글 작성</h1>
    <div v-if="isLoading">로딩 중...</div>
    <div v-else>
      <form @submit.prevent="createPost">
        <div class="form-group">
          <label for="menuSelect">메뉴 선택 (필수)</label>
          <select v-model="newPost.menuId" class="form-control" id="menuSelect" required>
            <option value="" disabled>메뉴를 선택하세요</option>
            <option
                v-for="menu in allMenus"
                :key="menu.id"
                :value="menu.id"
                :disabled="isParentMenu(menu.id)"
            >
              {{ menu.name }} {{ menu.parentId ? `(하위: ${getParentName(menu.parentId)})` : '' }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label for="postTitle">제목</label>
          <input v-model="newPost.title" class="form-control" id="postTitle" required/>
        </div>
        <div class="form-group">
          <label for="postContent">내용</label>
          <textarea v-model="newPost.content" class="form-control" id="postContent" required></textarea>
        </div>
        <div class="form-group">
          <label for="postFiles">첨부 파일</label>
          <input type="file" multiple class="form-control" id="postFiles" @change="handleFileChange"/>
        </div>
        <button type="submit" class="btn btn-primary mt-2" :disabled="isPosting">작성하기</button>
        <router-link :to="`/blog/${blogUrl}`" class="btn btn-secondary mt-2 ml-2">취소</router-link>
      </form>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'PostCreate',
  components: {NavBar},
  props: {
    blogUrl: String, // URL에서 블로그 URL 가져오기
  },
  data() {
    return {
      blog: null,
      menus: [],
      allMenus: [], // 계층 구조를 평평하게 펼친 메뉴 목록
      isLoading: true,
      isPosting: false,
      newPost: {
        title: '',
        content: '',
        menuId: '',
        userId: null,
      },
      postFiles: [],
    };
  },
  async created() {
    await this.loadBlogAndMenus();
  },
  methods: {
    async loadBlogAndMenus() {
      try {
        this.isLoading = true;
        const blogUrl = this.blogUrl || this.$route.params.blogUrl;
        console.log('Fetching blog with URL:', blogUrl);

        this.blog = await authService.getBlogByUrl(blogUrl);
        console.log('Loaded blog:', JSON.stringify(this.blog));

        if (this.blog) {
          this.menus = await authService.getMenusByBlogId(this.blog.id);
          console.log('Loaded menus:', JSON.stringify(this.menus));
          this.allMenus = this.flattenMenus(this.menus);
          console.log('Flattened allMenus:', JSON.stringify(this.allMenus));

          const user = await authService.getCurrentUser();
          console.log('Current user:', JSON.stringify(user));
          if (user && user.id) {
            this.newPost.userId = user.id;
            if (this.blog.userId && this.blog.userId !== user.id) {
              throw new Error('이 블로그에 게시글을 작성할 권한이 없습니다.');
            }
          } else {
            throw new Error('사용자 정보가 없습니다.');
          }

          const queryMenuId = this.menuId || this.$route.query.menuId;
          if (queryMenuId && this.allMenus.some(menu => menu.id === Number(queryMenuId))) {
            this.newPost.menuId = Number(queryMenuId);
            console.log('Pre-selected menuId from query:', this.newPost.menuId);
          }

          // 부모 메뉴 확인 로그
          this.allMenus.forEach(menu => {
            console.log(`Menu ${menu.id} (${menu.name}) is parent: ${this.isParentMenu(menu.id)}`);
          });
        }
      } catch (error) {
        console.error('블로그 또는 메뉴 로드 실패:', error);
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
    },
    flattenMenus(menus) {
      const result = [];
      const flatten = (menuList) => {
        menuList.forEach(menu => {
          result.push(menu);
          if (menu.subMenus && menu.subMenus.length > 0) {
            flatten(menu.subMenus);
          }
        });
      };
      flatten(menus);
      return result;
    },
    getParentName(parentId) {
      const parent = this.allMenus.find(menu => menu.id === parentId);
      return parent ? parent.name : '';
    },
    isParentMenu(menuId) {
      return this.allMenus.some(menu => menu.parentId === menuId);
    },
    handleFileChange(event) {
      this.postFiles = Array.from(event.target.files);
      console.log('Selected files:', this.postFiles);
    },
    async createPost() {
      if (!this.newPost.menuId) {
        alert('메뉴를 선택해야 게시글을 작성할 수 있습니다.');
        return;
      }
      this.isPosting = true;
      try {
        const postData = {...this.newPost};
        const createdPost = await authService.createPost(postData, this.postFiles);
        console.log('Created post:', JSON.stringify(createdPost));
        this.$router.push(`/blog/${this.blog.url}/${this.newPost.menuId}`);
      } catch (error) {
        console.error('게시글 작성 실패:', error);
        alert('게시글 작성 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isPosting = false;
      }
    },
  },
};
</script>

<style scoped>
.post-create {
  max-width: 800px;
  margin: 0 auto;
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
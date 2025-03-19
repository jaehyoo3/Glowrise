<template>
  <div class="post-edit container mt-4">
    <NavBar/>
    <h1>게시글 수정</h1>
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="post">
      <form @submit.prevent="updatePost">
        <div class="form-group">
          <label for="menuSelect">메뉴 선택 (필수)</label>
          <select v-model="editedPost.menuId" class="form-control" id="menuSelect" required>
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
          <input v-model="editedPost.title" class="form-control" id="postTitle" required/>
        </div>
        <div class="form-group">
          <label for="postContent">내용</label>
          <textarea v-model="editedPost.content" class="form-control" id="postContent" required></textarea>
        </div>
        <div class="form-group">
          <label for="postFiles">첨부 파일</label>
          <input type="file" multiple class="form-control" id="postFiles" @change="handleFileChange"/>
          <p v-if="post.fileIds && post.fileIds.length > 0">현재 파일: {{ post.fileIds.length }}개</p>
        </div>
        <button type="submit" class="btn btn-primary mt-2" :disabled="isUpdating">수정하기</button>
        <router-link :to="`/${blogUrl}/${menuId}/${postId}`" class="btn btn-secondary mt-2 ml-2">취소</router-link>
      </form>
    </div>
    <div v-else>
      <h1>게시글을 찾을 수 없습니다.</h1>
      <router-link to="/" class="btn btn-primary">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'PostEdit',
  components: {NavBar},
  props: {
    blogUrl: String,
    menuId: String,
    postId: String,
  },
  data() {
    return {
      post: null,
      editedPost: {
        title: '',
        content: '',
        menuId: '',
        userId: null,
      },
      allMenus: [],
      isLoading: true,
      isUpdating: false,
      postFiles: [],
    };
  },
  async created() {
    console.log('PostEdit created with props:', this.blogUrl, this.menuId, this.postId);
    await this.loadPostAndMenus();
  },
  methods: {
    async loadPostAndMenus() {
      try {
        this.isLoading = true;
        const postResponse = await authService.getPostById(this.postId);
        console.log('Loaded post:', JSON.stringify(postResponse));
        this.post = postResponse;

        if (this.post) {
          const user = await authService.getCurrentUser();
          console.log('Current user:', JSON.stringify(user));
          if (this.post.userId && this.post.userId !== user.id) {
            throw new Error('이 게시글을 수정할 권한이 없습니다.');
          }
          this.editedPost = {...this.post, userId: user.id};
          const blog = await authService.getBlogByUrl(this.blogUrl);
          console.log('Loaded blog:', JSON.stringify(blog));
          const rawMenus = await authService.getMenusByBlogId(blog.id);
          this.allMenus = rawMenus.map(menu => ({...menu}));
          console.log('Loaded menus:', JSON.stringify(this.allMenus));

          // menuId 초기값 설정
          if (!this.editedPost.menuId && this.menuId) {
            this.editedPost.menuId = Number(this.menuId);
          }
        }
      } catch (error) {
        console.error('게시글 또는 메뉴 로드 실패:', error);
        this.post = null;
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
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
    async updatePost() {
      this.isUpdating = true;
      try {
        const postData = {...this.editedPost};
        console.log('Sending postData:', JSON.stringify(postData));
        const updatedPost = await authService.updatePost(this.post.id, postData, this.postFiles);
        console.log('Updated post:', JSON.stringify(updatedPost));
        this.$router.push({
          path: `/${this.blogUrl}/${updatedPost.menuId}/${this.post.id}`, // 수정된 menuId 사용
          query: {refresh: Date.now()}
        });
      } catch (error) {
        console.error('게시글 수정 실패:', error.response?.data || error.message);
        alert('게시글 수정 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isUpdating = false;
      }
    }
  },
};
</script>

<style scoped>
.post-edit {
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
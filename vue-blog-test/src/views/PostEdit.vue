<template>
  <div class="post-edit container mt-4">
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
import {mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import
// Quill 에디터 사용 시 관련 import 필요
//import { QuillEditor } from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';

export default {
  name: 'PostEdit',
  // components: { QuillEditor }, // Quill 사용 시 등록
  props: {
    blogUrl: String, // 라우터 prop
    menuId: String,  // 라우터 prop
    postId: String,  // 라우터 prop
  },
  data() {
    return {
      post: null, // 원본 게시글 데이터 (수정 전 비교용)
      editedPost: { // 수정될 데이터
        title: '',
        content: '',
        menuId: '', // menuId 타입 주의 (Number)
        userId: null, // 스토어에서 가져올 userId
        // 기존 파일 정보 등 추가 필요 시 여기에
      },
      allMenus: [], // 메뉴 선택 옵션 목록
      isLoading: true, // 데이터 로딩 상태
      isUpdating: false, // 수정 중 상태
      postFiles: [], // 새로 첨부할 파일 목록 (input type="file"과 연결)
    };
  },
  computed: {
    // --- Vuex Getters 매핑 ---
    ...mapGetters(['isLoggedIn', 'userId']) // 로그인 상태 및 사용자 ID
    // ------------------------
  },
  async created() {
    console.log('PostEdit created with props:', this.blogUrl, this.menuId, this.postId);
    // 컴포넌트 생성 시 게시글 및 메뉴 정보 로드
    await this.loadPostAndMenus();
  },
  methods: {
    // 게시글 및 메뉴 로드 (userId를 스토어에서 가져오도록 수정)
    async loadPostAndMenus() {
      this.isLoading = true;
      try {
        // 1. 게시글 정보 로드 (authService 사용 유지)
        if (!this.postId) throw new Error('게시글 ID가 없습니다.');
        console.log(`PostEdit: 게시글 정보 로드 중 (Post ID: ${this.postId})`);
        const postResponse = await authService.getPostById(this.postId);
        if (!postResponse) throw new Error(`게시글(${this.postId})을 찾을 수 없습니다.`);
        this.post = postResponse; // 원본 데이터 저장
        console.log('PostEdit: 원본 게시글 로드 완료');

        // 2. 스토어에서 사용자 ID 확인 및 소유권 검증
        if (!this.isLoggedIn || !this.userId) {
          throw new Error('로그인이 필요하거나 사용자 정보를 가져올 수 없습니다.');
        }
        // 소유권 확인
        if (this.post.userId && this.post.userId !== this.userId) {
          throw new Error('이 게시글을 수정할 권한이 없습니다.');
        }
        console.log(`PostEdit: 사용자 ID ${this.userId} 확인 및 수정 권한 확인 완료.`);

        // 3. 수정할 데이터(editedPost) 초기화 (원본 복사 및 userId 설정)
        this.editedPost = {
          ...this.post, // 원본 게시글 데이터 복사
          userId: this.userId // 스토어의 userId로 설정
        };
        // menuId 타입이 숫자여야 할 수 있으므로 변환
        this.editedPost.menuId = this.editedPost.menuId ? Number(this.editedPost.menuId) : null;

        // 4. 블로그 및 메뉴 정보 로드 (authService 사용 유지)
        if (!this.blogUrl) throw new Error('블로그 URL이 없습니다.');
        console.log(`PostEdit: 블로그 및 메뉴 정보 로드 중 (Blog URL: ${this.blogUrl})`);
        const blog = await authService.getBlogByUrl(this.blogUrl);
        if (!blog || !blog.id) throw new Error('블로그 정보를 찾을 수 없습니다.');
        const rawMenus = await authService.getMenusByBlogId(blog.id);
        // 메뉴 목록을 바로 select 옵션으로 사용 (PostCreate와 달리 계층 구조 표시 불필요 시)
        this.allMenus = rawMenus.map(menu => ({...menu})); // 단순 복사
        // 또는 PostCreate처럼 계층/평탄화 필요 시 해당 로직 사용
        // const hierarchicalMenus = this.buildMenuHierarchy(rawMenus);
        // this.allMenus = this.flattenMenus(hierarchicalMenus);
        console.log(`PostEdit: 메뉴 목록 로드 완료 (${this.allMenus.length}개)`);

        // 5. menuId 초기값 재확인 (라우터 prop 우선)
        // props로 menuId가 넘어왔다면 해당 값 사용, 아니면 로드된 post의 menuId 사용
        const initialMenuId = this.menuId ? Number(this.menuId) : this.editedPost.menuId;
        // 부모 메뉴가 아닌지 확인 후 설정 (isParentMenu 로직 필요)
        if (initialMenuId && !this.isParentMenu(initialMenuId)) {
          this.editedPost.menuId = initialMenuId;
        } else if (initialMenuId && this.isParentMenu(initialMenuId)) {
          console.warn(`PostEdit: 초기 메뉴 ID(${initialMenuId})가 부모 메뉴입니다. 선택을 해제합니다.`);
          this.editedPost.menuId = null; // 부모 메뉴면 선택 안 함
        }

      } catch (error) {
        console.error('PostEdit: 게시글 또는 메뉴 로드 실패:', error);
        alert('수정할 게시글 정보를 불러오는 데 실패했습니다: ' + error.message);
        this.post = null;
        // 이전 페이지 또는 블로그 홈으로 이동
        this.$router.go(-1) || this.$router.push(`/${this.blogUrl}`);
      } finally {
        this.isLoading = false;
      }
    },

    // 부모 메뉴 이름 찾기 (allMenus 사용)
    getParentName(parentId) {
      if (!parentId) return '';
      const parent = this.allMenus.find(menu => menu.id === parentId);
      return parent ? parent.name : '';
    },

    // 부모 메뉴인지 확인 (allMenus 사용, isParent 또는 subMenuIds 필드 확인 필요)
    isParentMenu(menuId) {
      return this.allMenus.some(m => m.parentId === menuId);
    },

    // 파일 변경 핸들러 (기존과 동일)
    handleFileChange(event) {
      this.postFiles = Array.from(event.target.files);
      console.log('PostEdit: 선택된 파일:', this.postFiles);
    },

    // 게시글 수정 (userId는 이미 editedPost에 설정됨)
    async updatePost() {
      // 유효성 검사 (제목, 내용, 메뉴 등)
      if (!this.editedPost.menuId) {
        alert('게시글을 등록할 메뉴를 선택해주세요.');
        return;
      }
      if (this.isParentMenu(Number(this.editedPost.menuId))) {
        alert('글은 하위 메뉴에만 등록할 수 있습니다.');
        return;
      }
      if (!this.editedPost.title.trim()) {
        alert('제목을 입력해주세요.');
        return;
      }
      // Quill 에디터 내용 확인 (Quill 사용 시)
      // const quill = this.$refs.quillEditorRef?.getQuill();
      // const textContent = quill ? quill.getText().trim() : (this.editedPost.content || '').trim();
      // if (!textContent && !this.editedPost.content.includes('<img')) { alert('내용을 입력해주세요.'); return; }

      this.isUpdating = true;
      try {
        // editedPost 객체 준비 (필요시 데이터 가공)
        const postData = {...this.editedPost};
        // userId는 이미 포함되어 있음
        console.log('PostEdit: 게시글 업데이트 요청 데이터:', JSON.stringify(postData));

        // authService 호출 (변경 없음)
        // updatePost 함수는 postId, postData, files 를 인자로 받음
        const updatedPost = await authService.updatePost(this.postId, postData, this.postFiles);
        console.log('PostEdit: 게시글 업데이트 성공:', updatedPost);

        // 성공 후 상세 페이지로 이동 (쿼리에 refresh 추가하여 데이터 강제 리로드)
        alert('게시글이 성공적으로 수정되었습니다.');
        this.$router.push({
          // 경로 구성 시 blogUrl, menuId(업데이트된 것), postId 사용
          path: `/${this.blogUrl}/${updatedPost.menuId}/${this.postId}`, // 수정된 menuId 사용
          query: {refresh: Date.now()} // 쿼리 파라미터 추가
        });
      } catch (error) {
        console.error('PostEdit: 게시글 수정 실패:', error.response?.data || error.message);
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
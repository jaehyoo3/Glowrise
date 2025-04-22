<template>
  <div class="post-editor">
    <div class="post-editor__container">
      <div class="post-editor__header">
        <h1>게시글 수정</h1>
      </div>

      <div v-if="isLoading" class="post-editor__loading">
        <span>로딩 중...</span>
      </div>

      <div v-else-if="post" class="post-editor__form">
        <form @submit.prevent="updatePost">
          <div class="form-group">
            <label for="menuSelect">메뉴 선택</label>
            <select id="menuSelect" v-model="editedPost.menuId" required>
              <option disabled value="">메뉴를 선택하세요</option>
              <option
                  v-for="menu in allMenus"
                  :key="menu.id"
                  :disabled="isParentMenu(menu.id)"
                  :value="menu.id"
              >
                {{ menu.name }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label for="postTitle">제목</label>
            <input id="postTitle" v-model="editedPost.title" required/>
          </div>

          <div class="form-group">
            <label for="postContent">내용</label>
            <QuillEditor
                id="postContent"
                ref="quillEditorRef"
                v-model:content="editedPost.content"
                :toolbar="toolbarOptions"
                contentType="html"
                theme="snow"
                @ready="onEditorReady"
            />
          </div>

          <div class="form-group">
            <label for="postFiles">첨부 파일</label>
            <div class="file-upload">
              <input id="postFiles" multiple type="file" @change="handleFileChange"/>
              <div v-if="post.fileIds && post.fileIds.length > 0" class="file-upload__info">
                현재 파일: {{ post.fileIds.length }}개
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button :disabled="isUpdating" class="btn btn-primary" type="submit">
              {{ isUpdating ? '수정 중...' : '수정하기' }}
            </button>
            <router-link :to="`/${blogUrl}/${menuId}/${postId}`" class="btn btn-secondary">
              취소
            </router-link>
          </div>
        </form>
      </div>

      <div v-else class="post-editor__not-found">
        <h2>게시글을 찾을 수 없습니다.</h2>
        <router-link class="btn btn-secondary" to="/">홈으로 돌아가기</router-link>
      </div>
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
.post-editor {
  background-color: #f9f9f9;
  min-height: 100vh;
  padding: 40px 0;
}

.post-editor__container {
  max-width: 900px;
  margin: 0 auto;
  background-color: #ffffff;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}

.post-editor__header {
  padding: 28px 40px;
  border-bottom: 1px solid #e8e8e8;
}

.post-editor__header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.post-editor__loading {
  padding: 60px 0;
  text-align: center;
  color: #666;
  font-size: 16px;
}

.post-editor__form {
  padding: 40px;
}

.post-editor__not-found {
  padding: 60px 40px;
  text-align: center;
}

.post-editor__not-found h2 {
  margin-bottom: 20px;
  color: #555;
}

.form-group {
  margin-bottom: 30px;
}

.form-group label {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 15px;
  transition: border-color 0.2s;
  background-color: #fff;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #808080;
}

.form-group select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23555' d='M2 4l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 40px;
}

.file-upload {
  position: relative;
}

.file-upload input[type="file"] {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #f7f7f7;
  cursor: pointer;
}

.file-upload__info {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.btn {
  padding: 12px 24px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-primary {
  background-color: #4a4a4a;
  color: white;
}

.btn-primary:hover {
  background-color: #333333;
}

.btn-primary:disabled {
  background-color: #c0c0c0;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #e9e9e9;
  color: #4a4a4a;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-secondary:hover {
  background-color: #d9d9d9;
}

/* Quill 에디터 스타일 */
:deep(.ql-toolbar.ql-snow) {
  border: 1px solid #e0e0e0;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  background-color: #f7f7f7;
}

:deep(.ql-container.ql-snow) {
  border: 1px solid #e0e0e0;
  border-top: none;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  min-height: 300px;
}

:deep(.ql-editor) {
  min-height: 300px;
}

/* 반응형 조정 */
@media (max-width: 960px) {
  .post-editor__container {
    margin: 0 20px;
  }

  .post-editor__header,
  .post-editor__form {
    padding: 20px;
  }
}
</style>
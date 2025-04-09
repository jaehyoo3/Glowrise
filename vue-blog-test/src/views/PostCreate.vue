<template>
  <div class="post-create">
    <div class="container">
      <div class="post-create-content">
        <div class="post-create-header">
          <h1>새 게시글 작성</h1>
        </div>

        <div v-if="isLoading" class="loading-state">
          <span>로딩 중...</span>
        </div>

        <div v-else class="post-form-container">
          <form @submit.prevent="createPost" class="post-form">
            <div class="form-group">
              <label for="menuSelect" class="form-label">메뉴 선택 (필수)</label>
              <select id="menuSelect" v-model="newPost.menuId" class="form-control" required>
                <option disabled style="display: none;" value="">메뉴를 선택하세요</option>
                <option v-for="menu in allMenus" :key="menu.id" :disabled="isParentMenu(menu.id)" :value="menu.id">
                  {{ menu.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label for="postTitle" class="form-label">제목</label>
              <input
                  v-model="newPost.title"
                  class="form-control"
                  id="postTitle"
                  required
                  placeholder="게시글 제목을 입력하세요"
              />
            </div>

            <div class="form-group">
              <label for="postContent" class="form-label">내용</label>
              <QuillEditor
                  ref="quillEditorRef"
                  v-model:content="newPost.content"
                  :toolbar="toolbarOptions"
                  contentType="html"
                  placeholder="게시글 내용을 작성하세요"
                  style="min-height: 250px; background-color: #f8f9fa;"
                  theme="snow"
                  id="postContent"
                  @ready="onEditorReady"
              />
            </div>

            <div class="form-actions">
              <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="isPosting"
              >
                {{ isPosting ? '작성 중...' : '작성하기' }}
              </button>
              <router-link
                  :to="`/${blogUrl}`"
                  class="btn btn-secondary"
              >
                취소
              </router-link>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import
import {QuillEditor} from '@vueup/vue-quill';
import '@vueup/vue-quill/dist/vue-quill.snow.css';

export default {
  name: 'PostCreate',
  components: {QuillEditor},
  props: {blogUrl: String}, // 라우터 prop
  data() {
    return {
      blog: null,      // 글을 작성할 블로그 정보
      menus: [],       // 메뉴 원본 DTO 리스트 (메뉴 구조 및 isParentMenu 확인용)
      allMenus: [],    // Select 옵션용 최종 평탄화/들여쓰기 목록
      isLoading: true, // 블로그/메뉴 로딩 상태
      isPosting: false, // 글 게시 중 상태
      newPost: {
        title: '',
        content: '',   // Quill 에디터 내용
        menuId: '',    // 선택된 메뉴 ID (초기값 빈 문자열 또는 null)
        userId: null,  // userId는 스토어에서 가져옴
      },
      inlineImageFileIds: [], // 본문 삽입 이미지 ID 목록
      toolbarOptions: [ // Quill 툴바 옵션 (기존과 동일)
        [{'header': [1, 2, 3, 4, 5, 6, false]}], /* ... 다른 옵션들 ... */ ['link', 'image'], ['clean']
      ],
    };
  },
  computed: {
    // --- Vuex Getters 매핑 ---
    ...mapGetters(['userId', 'isLoggedIn']) // 현재 사용자 ID 및 로그인 상태
    // ------------------------
  },
  async created() {
    // 컴포넌트 생성 시 블로그 및 메뉴 정보 로드
    await this.loadBlogAndMenus();
  },
  methods: {
    // 블로그 및 메뉴 로드 (userId를 스토어에서 가져오도록 수정)
    async loadBlogAndMenus() {
      this.isLoading = true;
      try {
        // 1. 스토어에서 userId 확인
        if (!this.isLoggedIn || !this.userId) {
          throw new Error('로그인이 필요하거나 사용자 ID를 가져올 수 없습니다.');
        }
        this.newPost.userId = this.userId; // newPost 객체에 userId 설정
        console.log(`PostCreate: 사용자 ID ${this.userId} 확인.`);

        // 2. 블로그 정보 로드 (authService.getBlogByUrl 사용 유지)
        const blogUrl = this.blogUrl || this.$route.params.blogUrl;
        if (!blogUrl) throw new Error('블로그 URL 없음');
        console.log(`PostCreate: 블로그 정보 로드 중 (URL: ${blogUrl})`);
        this.blog = await authService.getBlogByUrl(blogUrl);
        if (!this.blog || !this.blog.id) throw new Error(`블로그 찾기 실패: ${blogUrl}`);
        console.log(`PostCreate: 블로그 로드 성공 (ID: ${this.blog.id})`);

        // (선택적) 블로그 소유권 확인
        if (this.blog.userId !== this.userId) {
          console.warn('PostCreate: 현재 사용자가 이 블로그의 소유주가 아닙니다.');
          // 글쓰기 권한이 없다면 여기서 에러 처리 또는 리디렉션
          // alert('글을 작성할 권한이 없습니다.');
          // this.$router.push('/');
          // return;
        }

        // 3. 메뉴 정보 로드 및 처리 (authService 사용 유지)
        console.log(`PostCreate: 메뉴 로드 중 (Blog ID: ${this.blog.id})`);
        const flatMenuDtos = await authService.getMenusByBlogId(this.blog.id);
        this.menus = flatMenuDtos; // 원본 리스트 저장
        console.log(`PostCreate: 메뉴 원본 DTO 로드 (${this.menus.length}개)`);
        const hierarchicalMenus = this.buildMenuHierarchy(flatMenuDtos);
        this.allMenus = this.flattenMenus(hierarchicalMenus); // 옵션용 목록 생성
        console.log(`PostCreate: 메뉴 옵션 목록 생성 (${this.allMenus.length}개)`);

        // 4. 쿼리 파라미터로 전달된 menuId 처리 (기존 로직 유지)
        const queryMenuId = this.$route.query.menuId;
        if (queryMenuId && this.allMenus.some(menu => menu.id === Number(queryMenuId))) {
          // 부모 메뉴가 아니면 초기 선택값으로 설정
          if (!this.isParentMenu(Number(queryMenuId))) {
            this.newPost.menuId = Number(queryMenuId);
            console.log(`PostCreate: 쿼리 파라미터 메뉴 ID (${queryMenuId}) 설정됨.`);
          } else {
            console.log(`PostCreate: 쿼리 파라미터 메뉴 ID (${queryMenuId})는 부모 메뉴라 설정 안 함.`);
          }
        }

      } catch (error) {
        console.error('PostCreate: 블로그/메뉴 로드 실패:', error);
        alert('글 작성 페이지 로딩 중 오류가 발생했습니다: ' + error.message);
        // 에러 발생 시 이전 페이지 또는 홈으로 이동
        this.$router.go(-1) || this.$router.push('/');
      } finally {
        this.isLoading = false;
      }
    },

    onEditorReady(quill) {
      console.log('PostCreate: Quill 에디터 준비 완료 (onEditorReady)', quill);
      const toolbar = quill.getModule('toolbar');
      if (toolbar) {
        toolbar.addHandler('image', this.handleImageUpload); // 이미지 핸들러 등록
        console.log('Quill 이미지 핸들러 등록 완료 (onEditorReady)');
      } else {
        console.error('Quill 툴바 모듈 찾기 실패 (onEditorReady).');
      }
    },

    // 메뉴 평탄화 함수 (계층 구조 -> Select 옵션 목록) (기존과 동일)
    flattenMenus(hierarchicalMenus) {
      const result = [];
      const flatten = (menuList, depth = 0) => {
        if (!Array.isArray(menuList)) return;
        menuList.forEach(menu => {
          if (!menu) return;
          const prefix = depth > 0 ? '\u00A0'.repeat(depth * 4) : ''; // 들여쓰기
          result.push({...menu, name: prefix + (menu.name || '')}); // 이름에 prefix 추가
          if (menu.subMenus && menu.subMenus.length > 0) {
            flatten(menu.subMenus, depth + 1); // 재귀 호출
          }
        });
      };
      flatten(hierarchicalMenus);
      return result;
    },

    // 부모 메뉴 이름 찾기 (원본 menus 리스트 사용) (기존과 동일)
    getParentName(parentId) {
      if (!parentId) return '';
      const parentDto = this.menus.find(menu => menu.id === parentId);
      return parentDto ? parentDto.name : '';
    },

    // 부모 메뉴인지 확인 (원본 menus 리스트 사용) (기존과 동일)
    isParentMenu(menuId) {
      const menuDto = this.menus.find(menu => menu.id === menuId);
      // 백엔드 DTO에 isParent 또는 subMenuIds 같은 필드가 있는지 확인 필요
      // 예시: 하위 메뉴 ID 목록(subMenuIds) 존재 여부로 판단
      return menuDto ? (menuDto.subMenuIds && menuDto.subMenuIds.length > 0) : false;
      // 또는 백엔드에서 isParent 플래그를 직접 준다면: return menuDto?.isParent || false;
    },

    // 평탄화된 메뉴 DTO 리스트 -> 계층 구조 변환 (기존과 동일)
    buildMenuHierarchy(flatList) {
      const map = {};
      const roots = [];
      if (!flatList || !Array.isArray(flatList)) return roots;
      flatList.forEach(menu => {
        map[menu.id] = {...menu, subMenus: []};
      });
      flatList.forEach(menu => {
        if (!menu || !map[menu.id]) return;
        if (menu.parentId && map[menu.parentId]) {
          map[menu.parentId].subMenus.push(map[menu.id]);
        } else {
          roots.push(map[menu.id]);
        }
      });
      const sortByOrderIndex = (a, b) => (a.orderIndex || 0) - (b.orderIndex || 0);
      roots.sort(sortByOrderIndex);
      Object.values(map).forEach(menu => {
        menu.subMenus?.sort(sortByOrderIndex);
      });
      return roots;
    },

    // 게시글 생성 (userId는 이미 설정됨) (기존과 동일)
    async createPost() {
      // 유효성 검사
      if (!this.newPost.menuId) {
        alert('게시글을 등록할 메뉴를 선택해주세요.');
        return;
      }
      if (this.isParentMenu(Number(this.newPost.menuId))) {
        alert('글은 하위 메뉴에만 등록할 수 있습니다.');
        return;
      }
      if (!this.newPost.title.trim()) {
        alert('제목을 입력해주세요.');
        return;
      }
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = this.newPost.content;
      const textContent = tempDiv.textContent || tempDiv.innerText || "";
      if (!textContent.trim() && !this.newPost.content.includes('<img')) {
        alert('내용을 입력해주세요.');
        return;
      }

      this.isPosting = true;
      try {
        // userId는 data() 또는 loadBlogAndMenus에서 이미 설정됨
        const postData = {...this.newPost, inlineImageFileIds: this.inlineImageFileIds};
        // authService 호출 (변경 없음)
        const createdPost = await authService.createPost(postData);
        console.log('PostCreate: 게시글 생성 성공:', createdPost);
        // 성공 후 블로그 뷰로 이동
        alert('게시글이 성공적으로 등록되었습니다.');
        this.$router.push(`/${this.blog.url}`); // 생성된 글로 바로 가는 대신 블로그 홈으로 이동
      } catch (error) {
        console.error('PostCreate: 게시글 생성 실패:', error);
        alert('게시글 등록 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isPosting = false;
      }
    },

    // Quill 이미지 업로드 핸들러 (기존과 동일)
    handleImageUpload() {
      console.log('--- handleImageUpload 함수 시작됨 ---');
      const input = document.createElement('input');
      input.setAttribute('type', 'file');
      input.setAttribute('accept', 'image/*');
      input.click();

      input.onchange = async () => {
        const file = input.files[0];
        if (!file) return;

        const maxSize = 10 * 1024 * 1024; // 예: 10MB
        if (file.size > maxSize) {
          alert(`파일 크기는 ${maxSize / 1024 / 1024}MB를 초과할 수 없습니다.`);
          return; // 업로드 중단
        }

        const formData = new FormData();
        formData.append('image', file); // 백엔드에서 받을 때 사용할 이름 확인 (예: 'image')

        const quill = this.$refs.quillEditorRef?.getQuill();
        const range = quill ? quill.getSelection(true) : null;
        let loadingTextRemoved = false; // 로딩 텍스트 제거 여부 플래그

        // 사용자에게 로딩 중임을 표시
        if (range && quill) {
          quill.insertText(range.index, '\n[이미지 업로드 중...]\n', {color: 'grey'});
          quill.disable(); // 에디터 비활성화
        }

        try {
          // authService 통해 이미지 업로드 API 호출
          const responseData = await authService.uploadInlineImage(formData);
          const {imageUrl, fileId} = responseData; // 백엔드 응답 필드 확인

          if (imageUrl && fileId && quill && range) {
            // 로딩 텍스트 제거 (반드시 수행)
            const loadingTextLength = '\n[이미지 업로드 중...]\n'.length;
            quill.deleteText(range.index, loadingTextLength);
            loadingTextRemoved = true;

            // 에디터에 이미지 삽입
            quill.insertEmbed(range.index, 'image', imageUrl);
            quill.setSelection(range.index + 1); // 이미지 다음으로 커서 이동

            // 업로드된 이미지 파일 ID 추적 (게시글 저장 시 함께 전송)
            this.inlineImageFileIds.push(fileId);
            console.log(`Quill: 이미지 삽입 완료: ${imageUrl}, File ID: ${fileId}`);
          } else {
            // 응답 데이터가 유효하지 않거나 에디터/범위 정보가 없는 경우
            throw new Error("이미지 URL, 파일 ID 또는 에디터 정보를 가져올 수 없습니다.");
          }
        } catch (error) {
          console.error('Quill 이미지 업로드 실패:', error);
          alert('이미지 업로드 중 오류가 발생했습니다.');
          // 실패 시 로딩 텍스트 제거 (제거되지 않았다면)
          if (range && quill && !loadingTextRemoved) {
            const loadingTextLength = '\n[이미지 업로드 중...]\n'.length;
            quill.deleteText(range.index, loadingTextLength);
          }
        } finally {
          // 항상 에디터 활성화
          if (quill) quill.enable();
        }
      }; // input.onchange 끝
    }, // handleImageUpload 끝
  }, // methods 끝
};
</script>

<style scoped>
/* 전체 스타일 */
.post-create {
  background-color: #f8f9fa;
  min-height: 100vh;
  color: #333;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.post-create-content {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.post-create-header {
  margin-bottom: 2rem;
  text-align: center;
}

.post-create-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  border-bottom: 2px solid #000;
  padding-bottom: 0.5rem;
}

.loading-state {
  text-align: center;
  padding: 2rem;
  font-size: 1.2rem;
}

.post-form-container {
  max-width: 700px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.form-control {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  background-color: #f8f9fa;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
}

.form-control:focus {
  outline: none;
  border-color: #000;
}

.form-control::placeholder {
  color: #888;
}

.form-control select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='none' stroke='%23333' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M2 5l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 16px 12px;
  padding-right: 2.5rem;
}

.form-control::-ms-expand {
  display: none;
}

/* 부모 메뉴 비활성화 시 스타일 (옵션) */
.form-control option:disabled {
  color: #aaa;
  background-color: #eee;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
}

.btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  text-align: center;
  font-size: 1rem;
  border: 1px solid transparent;
  cursor: pointer;
}

.btn-primary {
  background-color: #000;
  color: white;
  border-color: #000;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #f8f9fa;
  color: #000;
  border: 1px solid #000;
}

.btn-secondary:hover {
  background-color: #e2e6ea;
}

/* Quill 관련 스타일 */
.form-group .ql-toolbar.ql-snow {
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
  border: 1px solid #ddd;
  border-bottom: 0;
  background-color: #f8f9fa;
  padding: 8px;
  box-sizing: border-box;
}

.form-group .ql-container.ql-snow {
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  border: 1px solid #ddd;
  box-sizing: border-box;
}

.form-group .ql-editor {
  min-height: 250px;
  background-color: white;
  font-size: 1rem;
  line-height: 1.6;
  padding: 12px 15px;
  box-sizing: border-box;
}

.form-group .ql-container.ql-snow:focus-within {
  border-color: #000;
}

.form-group .ql-snow .ql-picker-label {
  font-size: 14px;
}

.form-group .ql-snow .ql-stroke {
  stroke: #444;
}

.form-group .ql-snow .ql-fill {
  fill: #444;
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .post-create-content {
    padding: 1.5rem;
  }

  .post-create-header h1 {
    font-size: 2rem;
  }

  .form-actions {
    flex-direction: column;
    gap: 0.5rem;
  }

  .btn {
    width: 100%;
  }
}
</style>
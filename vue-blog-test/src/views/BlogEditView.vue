<template>
  <div class="blog-edit">
    <div class="container">
      <div class="blog-edit-content">
        <div class="blog-edit-header">
          <h1>블로그 수정</h1>
          <p>당신의 블로그를 개선하세요</p>
        </div>

        <form @submit.prevent="updateBlog" class="blog-edit-form">
          <div class="form-group">
            <label for="title">블로그 제목</label>
            <input
                type="text"
                id="title"
                v-model="blog.title"
                placeholder="블로그 제목을 입력하세요"
                required
                class="form-input"
            >
          </div>

          <div class="form-group">
            <label for="description">설명</label>
            <textarea
                id="description"
                v-model="blog.description"
                placeholder="블로그에 대한 설명을 입력하세요"
                class="form-input"
                rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="url">URL</label>
            <input
                type="text"
                id="url"
                v-model="blog.url"
                placeholder="고유한 URL을 입력하세요 (영문, 숫자, -만 가능)"
                @blur="checkUrl"
                required
                class="form-input"
                :class="{ 'input-error': urlError, 'input-success': urlAvailable }"
            >
            <div v-if="urlError" class="input-feedback error">
              {{ urlError }}
            </div>
            <div v-else-if="urlAvailable" class="input-feedback success">
              사용 가능한 URL입니다.
            </div>
          </div>

          <div class="form-actions">
            <button
                type="submit"
                class="submit-button"
                :disabled="isLoading || !urlAvailable"
            >
              {{ isLoading ? '수정 중...' : '수정하기' }}
            </button>
          </div>
        </form>

        <div class="menu-management">
          <h2>메뉴 관리</h2>
          <form @submit.prevent="addMenu" class="menu-add-form">
            <div class="form-group">
              <label for="menuName">메뉴 이름</label>
              <input
                  type="text"
                  id="menuName"
                  v-model="newMenu.name"
                  placeholder="새 메뉴 이름"
                  required
                  class="form-input"
              >
            </div>
            <button type="submit" class="add-menu-button">메뉴 추가</button>
          </form>

          <div v-if="isLoading" class="loading-state">
            <span>로딩 중...</span>
          </div>

          <div v-else-if="!menus.length" class="empty-state">
            <p>메뉴가 없습니다. 첫 메뉴를 추가해보세요.</p>
          </div>

          <div v-else class="menu-list">
            <draggable
                v-model="menus"
                item-key="id"
                @end="onDragEnd"
                group="menus"
                tag="div"
                :animation="200"
            >
              <template #item="{ element: menu }">
                <div
                    class="menu-item"
                    :class="{ 'sub-menu': menu.parentId }"
                >
                  <span>{{ menu.name }}</span>
                  <small v-if="menu.parentId" class="parent-info">
                    (상위 메뉴: {{ getParentName(menu.parentId) }})
                  </small>
                </div>
              </template>
            </draggable>

            <div class="menu-actions">
              <button
                  type="button"
                  class="save-order-button"
                  @click="saveMenuOrder"
                  :disabled="!orderChanged"
              >
                메뉴 순서 저장
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {defineComponent} from 'vue';
import {mapActions, mapGetters, mapState} from 'vuex'; // Vuex 헬퍼 함수 import
import Draggable from 'vuedraggable';
import authService from '@/services/authService'; // API 호출용 서비스 import

export default defineComponent({
  name: 'BlogEditView',
  components: {
    Draggable
  },
  data() {
    return {
      // blog 데이터는 스토어에서 가져오므로, 로컬에서는 복사본 또는 빈 객체로 시작
      blog: {title: '', description: '', url: '', id: null},
      isLoading: false, // 페이지 로딩 상태
      isSaving: false, // 저장 중 상태
      urlAvailable: true, // URL 사용 가능 여부 (초기값 true, 수정 시 확인)
      urlError: '',
      menus: [], // 메뉴 목록
      newMenu: {name: '', blogId: null, orderIndex: null, parentId: null}, // 새 메뉴 입력 폼
      orderChanged: false, // 메뉴 순서 변경 여부
    };
  },
  computed: {
    // --- Vuex State/Getters 매핑 ---
    ...mapState(['userBlog']), // 스토어의 userBlog 상태 직접 접근
    ...mapGetters(['userId', 'hasBlog']), // userId, 블로그 존재 여부 getter
    // ---------------------------
  },
  watch: {
    // 스토어의 userBlog 상태가 변경되면 로컬 blog 데이터 업데이트 및 메뉴 로드
    userBlog: {
      handler(newUserBlog) {
        if (newUserBlog && !newUserBlog.notFound) {
          console.log('BlogEditView: 스토어 userBlog 변경 감지', newUserBlog);
          this.blog.id = newUserBlog.id;
          this.blog.title = newUserBlog.title || '';
          this.blog.description = newUserBlog.description || '';
          this.blog.url = newUserBlog.url || '';
          this.newMenu.blogId = this.blog.id; // 새 메뉴 blogId 설정
          this.urlAvailable = true; // 이미 존재하는 블로그이므로 true
          this.urlError = '';
          // 블로그 정보 로드 후 메뉴 로드
          this.loadMenus();
        } else if (newUserBlog?.notFound) {
          console.log('BlogEditView: 스토어 userBlog가 notFound 상태.');
          // 블로그가 없는 경우 생성 페이지로 리디렉션
          this.$router.replace('/blog/create');
        } else {
          console.log('BlogEditView: 스토어 userBlog가 null 또는 로드되지 않음.');
          // 데이터가 아직 로드되지 않았을 수 있음. fetchUserBlog 시도?
          // App.vue 등 상위에서 이미 로드를 시도하므로 여기서는 기다리거나 로딩 표시
        }
      },
      immediate: true // 컴포넌트 생성 시 즉시 실행
    }
  },
  created() {
    // --- 삭제: loadBlog 호출 제거 ---
    // 스토어 상태 변경을 watch 에서 감지하여 처리
    // 만약 스토어에 블로그 정보가 없을 경우 로드 시도 (선택적)
    // if (!this.userBlog && this.userId) {
    //   console.log('BlogEditView: 스토어에 블로그 정보 없음, 로드 시도.');
    //   this.isLoading = true; // 로딩 상태 표시
    //   this.fetchUserBlog().finally(() => { this.isLoading = false; });
    // }
  },
  methods: {
    // --- Vuex Actions 매핑 ---
    ...mapActions(['fetchUserBlog']), // 블로그 정보 갱신용 액션
    // ------------------------

    // --- 삭제: loadBlog 메서드 ---
    // (스토어 watch 로직으로 대체됨)

    async updateBlog() {
      // URL 유효성 및 사용 가능 여부 최종 확인 (수정 시)
      if (this.urlError && !this.urlAvailable) {
        alert('URL을 확인해주세요.');
        return;
      }
      if (!this.blog.title) {
        alert('블로그 제목을 입력해주세요.');
        return;
      }

      if (!this.blog.id) return; // 블로그 ID 없으면 중단

      this.isSaving = true;
      try {
        // authService 직접 호출하여 블로그 정보 업데이트
        await authService.updateBlog(this.blog.id, {
          title: this.blog.title,
          description: this.blog.description,
          url: this.blog.url
        });
        alert('블로그가 수정되었습니다!');

        // --- Vuex: 수정 성공 후 스토어 상태 갱신 ---
        await this.fetchUserBlog();
        // -----------------------------------------

        // 수정된 블로그 URL로 이동 (갱신된 스토어 데이터 사용)
        // watch가 상태 변경을 감지하여 URL도 업데이트할 것임
        // this.$router.push(`/${this.blog.url}`); // 또는 스토어 getter 사용
        // URL 변경 시 페이지 이동이 필요할 수 있음
      } catch (error) {
        alert('블로그 수정 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSaving = false;
      }
    },
    async checkUrl() {
      // URL 중복 확인 로직 (기존과 동일)
      // 단, 현재 블로그의 원래 URL과 동일하면 확인할 필요 없음 (개선 가능)
      const originalUrl = this.userBlog?.url; // 스토어에서 원본 URL 가져오기
      if (this.blog.url === originalUrl) {
        this.urlAvailable = true;
        this.urlError = '';
        return;
      }

      this.urlError = '';
      this.urlAvailable = false;
      const urlPattern = /^[a-zA-Z0-9-]+$/;
      if (!this.blog.url) {
        this.urlError = 'URL을 입력해주세요.';
        return;
      }
      if (!urlPattern.test(this.blog.url)) {
        this.urlError = 'URL은 영문, 숫자, 하이픈(-)만 사용할 수 있습니다.';
        return;
      }
      try {
        const available = await authService.checkBlogUrlAvailability(this.blog.url);
        this.urlAvailable = available;
        if (!available) {
          this.urlError = '이미 사용 중인 URL입니다.';
        } else {
          this.urlError = '사용 가능한 URL입니다.';
        }
      } catch (error) {
        this.urlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },
    async loadMenus() {
      // 메뉴 로드 로직 (기존과 동일, authService 사용)
      if (!this.blog.id) return; // 블로그 ID 없으면 로드 불가

      this.isLoading = true; // 로딩 상태는 유지
      try {
        const response = await authService.getMenusByBlogId(this.blog.id);
        // 데이터 처리 및 정렬 (기존과 동일)
        this.menus = Array.isArray(response)
            ? response.map(menu => ({
              id: menu.id,
              name: menu.name || '',
              orderIndex: menu.orderIndex ?? 0,
              parentId: menu.parentId || null,
            })).sort((a, b) => a.orderIndex - b.orderIndex)
            : [];
      } catch (error) {
        console.error('Failed to load menus:', error);
        this.menus = []; // 에러 시 빈 배열
      } finally {
        this.isLoading = false;
      }
    },
    async addMenu() {
      // 새 메뉴 추가 로직 (기존과 동일)
      if (!this.newMenu.name || !this.newMenu.blogId) {
        alert('메뉴 이름을 입력해주세요.');
        return;
      }
      this.isSaving = true; // 저장 상태 표시
      try {
        // orderIndex 설정 (목록 끝)
        this.newMenu.orderIndex = this.menus.length;
        // authService 호출
        const createdMenu = await authService.createMenu(this.newMenu);

        // 메뉴 목록 즉시 업데이트 (API 응답 기반)
        this.menus.push({
          id: createdMenu.id,
          name: createdMenu.name || this.newMenu.name,
          orderIndex: createdMenu.orderIndex ?? this.menus.length - 1,
          parentId: createdMenu.parentId || this.newMenu.parentId,
        });

        // 입력 필드 초기화
        this.newMenu.name = '';
        this.newMenu.parentId = null; // parentId도 초기화
        // --- 메뉴 추가 후 목록 다시 로드 (선택적, 순서 등 확실히 하려면) ---
        // await this.loadMenus();
        // ---------------------------------------------------------

      } catch (error) {
        console.error('Menu addition failed:', error.response?.data || error.message);
        alert('메뉴 추가 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSaving = false;
      }
    },
    onDragEnd(event) {
      // 드래그 종료 시 순서/부모 변경 로직 (기존과 동일)
      const {oldIndex, newIndex} = event;
      if (oldIndex === newIndex) return;

      // 순서 변경 플래그 설정
      this.orderChanged = true;

      // 배열 내 요소 이동 (vuedraggable이 이미 처리했을 수 있으나, 인덱스 재계산 위해 필요)
      const movedItem = this.menus.splice(oldIndex, 1)[0];
      this.menus.splice(newIndex, 0, movedItem);

      // 모든 메뉴의 orderIndex 업데이트
      this.menus.forEach((menu, idx) => {
        menu.orderIndex = idx;
      });

      // 부모 ID 업데이트 로직 (필요한 경우 개선/확인 필요)
      // 예: 드롭된 위치의 이전 아이템을 부모로 설정? (vuedraggable 설정에 따라 다름)
      // if (newIndex > 0) {
      //   const draggedMenu = this.menus[newIndex];
      //   const potentialParent = this.menus[newIndex - 1];
      //   // 부모 설정 로직 구현 (단순히 이전 아이템으로 할지, depth 고려할지 등)
      // } else {
      //   this.menus[newIndex].parentId = null; // 최상위로 이동 시 부모 null
      // }
    },
    async saveMenuOrder() {
      // 메뉴 순서 저장 로직 (기존과 동일)
      if (!this.orderChanged) {
        // alert('변경된 사항이 없습니다.'); // 메시지 없이 조용히 종료 가능
        return;
      }
      this.isSaving = true;
      try {
        // authService 호출
        await authService.updateMenuOrder(this.blog.id, this.menus.map(m => ({
          id: m.id,
          orderIndex: m.orderIndex,
          parentId: m.parentId // parentId도 함께 전송
        })));
        alert('메뉴 순서가 저장되었습니다.');
        this.orderChanged = false; // 변경 플래그 리셋
        // --- 순서 저장 후 목록 다시 로드 (서버에서 ID 등 변경 가능성 대비) ---
        await this.loadMenus();
        // ---------------------------------------------------------
      } catch (error) {
        console.error('Failed to update menu order:', error.response?.data || error.message);
        alert('메뉴 순서 저장 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSaving = false;
      }
    },
    getParentName(parentId) {
      // 부모 메뉴 이름 찾는 함수 (기존과 동일)
      if (!parentId) return '없음';
      const parent = this.menus.find(menu => menu.id === parentId);
      return parent ? parent.name : '알 수 없음';
    },
  },
});
</script>

<style scoped>
.blog-edit {
  background-color: #f8f9fa;
  min-height: 100vh;
  color: #333;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.blog-edit-content {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  padding: 2rem;
}

.blog-edit-header {
  text-align: center;
  margin-bottom: 2rem;
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 1rem;
}

.blog-edit-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  margin-bottom: 0.5rem;
}

.blog-edit-header p {
  color: #666;
  font-size: 1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
}

.form-input:focus {
  outline: none;
  border-color: #000;
}

.form-input.input-error {
  border-color: #dc3545;
}

.form-input.input-success {
  border-color: #28a745;
}

.input-feedback {
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

.input-feedback.error {
  color: #dc3545;
}

.input-feedback.success {
  color: #28a745;
}

.form-actions {
  margin-top: 2rem;
  text-align: center;
}

.submit-button {
  background-color: #000;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.menu-management {
  margin-top: 2rem;
  border-top: 1px solid #e5e5e5;
  padding-top: 2rem;
}

.menu-management h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1rem;
  text-align: center;
}

.menu-add-form {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.menu-add-form .form-group {
  flex-grow: 1;
  margin-bottom: 0;
}

.add-menu-button {
  background-color: #f8f9fa;
  color: #000;
  border: 1px solid #000;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.add-menu-button:hover {
  background-color: #000;
  color: white;
}

.menu-list {
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 1rem;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background-color: white;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  margin-bottom: 0.5rem;
  cursor: move;
}

.menu-item.sub-menu {
  margin-left: 20px;
  background-color: #f0f0f0;
}

.parent-info {
  color: #666;
  font-size: 0.8rem;
}

.menu-actions {
  margin-top: 1rem;
  text-align: center;
}

.save-order-button {
  background-color: #000;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.save-order-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 1rem;
  color: #666;
}

@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .blog-edit-content {
    padding: 1rem;
  }

  .blog-edit-header h1 {
    font-size: 2rem;
  }

  .menu-add-form {
    flex-direction: column;
  }

  .add-menu-button {
    width: 100%;
    margin-top: 0.5rem;
  }
}
</style>
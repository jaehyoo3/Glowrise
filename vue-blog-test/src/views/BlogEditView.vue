<template>
  <div class="blog-edit">
    <div class="container">
      <div class="blog-edit-content">
        <div class="blog-edit-header">
          <h1>블로그 수정</h1>
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
                rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="url">URL</label>
            <div class="url-input-wrapper">
              <span class="url-prefix">blog.com/</span>
              <input
                  id="url"
                  v-model="blog.url"
                  :class="{ 'input-error': urlError, 'input-success': urlAvailable && blog.url }"
                  class="form-input url-input"
                  placeholder="your-blog-name"
                  required
                  type="text"
                  @blur="checkUrl"
              >
            </div>
            <div v-if="urlError" class="input-feedback error">
              <span class="feedback-icon">!</span> {{ urlError }}
            </div>
            <div v-else-if="urlAvailable && blog.url" class="input-feedback success">
              <span class="feedback-icon">✓</span> 사용 가능한 URL입니다.
            </div>
          </div>

          <div class="form-actions">
            <button
                type="submit"
                class="submit-button"
                :class="{ 'button-loading': isSaving }"
                :disabled="isLoading || !urlAvailable || !blog.title"
            >
              <span v-if="isSaving">수정 중...</span>
              <span v-else>블로그 수정하기</span>
            </button>
          </div>
        </form>

        <div class="menu-management">
          <h2>메뉴 관리</h2>

          <div class="menu-add-section">
            <form class="menu-add-form" @submit.prevent="addMenu">
              <div class="form-group">
                <label for="menuName">메뉴 이름</label>
                <input
                    id="menuName"
                    v-model="newMenu.name"
                    class="form-input"
                    placeholder="새 메뉴 이름"
                    required
                    type="text"
                >
              </div>
              <div class="form-group">
                <label for="parentMenu">상위 메뉴 (선택)</label>
                <select id="parentMenu" v-model="newMenu.parentId" class="form-input">
                  <option :value="null">-- 최상위 메뉴 --</option>
                  <option v-for="menu in topLevelMenus" :key="menu.id" :value="menu.id">
                    {{ menu.name }}
                  </option>
                </select>
              </div>
              <button
                  :class="{ 'button-loading': isSaving }"
                  :disabled="isSaving || !newMenu.name"
                  class="add-menu-button"
                  type="submit"
              >
                <span v-if="isSaving">추가 중...</span>
                <span v-else>메뉴 추가</span>
              </button>
            </form>
          </div>

          <div class="menu-content">
            <div v-if="isLoading" class="loading-state">
              <div class="loading-spinner"></div>
              <span>메뉴 로딩 중...</span>
            </div>

            <div v-else-if="!menus.length" class="empty-state">
              <div class="empty-state-icon">📋</div>
              <p>등록된 메뉴가 없습니다.</p>
              <p class="empty-state-desc">첫 메뉴를 추가해보세요.</p>
            </div>

            <div v-else class="menu-list">
              <h3>메뉴 순서 변경</h3>
              <p class="drag-instruction">아래 메뉴를 드래그하여 순서를 변경할 수 있습니다.</p>

              <draggable
                  v-model="menus"
                  :animation="200"
                  class="draggable-container"
                  group="menus"
                  item-key="id"
                  tag="div"
                  @end="onDragEnd"
              >
                <template #item="{ element: menu }">
                  <div
                      :class="{ 'sub-menu': menu.parentId }"
                      class="menu-item"
                  >
                    <div class="menu-item-content">
                      <div class="menu-item-handle">
                        <svg fill="none" height="18" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round"
                             stroke-width="2" viewBox="0 0 24 24" width="18" xmlns="http://www.w3.org/2000/svg">
                          <circle cx="9" cy="12" r="1"/>
                          <circle cx="9" cy="5" r="1"/>
                          <circle cx="9" cy="19" r="1"/>
                          <circle cx="15" cy="12" r="1"/>
                          <circle cx="15" cy="5" r="1"/>
                          <circle cx="15" cy="19" r="1"/>
                        </svg>
                      </div>
                      <div class="menu-item-name">
                        <span>{{ menu.name }}</span>
                        <small v-if="menu.parentId" class="parent-info">
                          (상위: {{ getParentName(menu.parentId) }})
                        </small>
                      </div>
                    </div>
                  </div>
                </template>
              </draggable>

              <div v-if="menus.length > 0" class="menu-actions">
                <button
                    :class="{ 'button-loading': isSaving && orderChanged }"
                    :disabled="!orderChanged || isSaving"
                    class="save-order-button"
                    type="button"
                    @click="saveMenuOrder"
                >
                  <span v-if="isSaving && orderChanged">저장 중...</span>
                  <span v-else>메뉴 순서 저장</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {defineComponent} from 'vue';
import {mapActions, mapGetters, mapState} from 'vuex';
import Draggable from 'vuedraggable';
import authService from '@/services/authService';

export default defineComponent({
  name: 'BlogEditView',
  components: {
    Draggable
  },
  data() {
    return {
      blog: {title: '', description: '', url: '', id: null},
      isLoading: false,
      isSaving: false,
      urlAvailable: true,
      urlError: '',
      menus: [],
      newMenu: {name: '', blogId: null, orderIndex: null, parentId: null},
      orderChanged: false,
    };
  },
  computed: {
    ...mapState(['userBlog']),
    ...mapGetters(['userId', 'hasBlog']),
    topLevelMenus() {
      return this.menus.filter(menu => !menu.parentId);
    }
  },
  watch: {
    userBlog: {
      handler(newUserBlog) {
        if (newUserBlog && !newUserBlog.notFound) {
          this.blog.id = newUserBlog.id;
          this.blog.title = newUserBlog.title || '';
          this.blog.description = newUserBlog.description || '';
          this.blog.url = newUserBlog.url || '';
          this.newMenu.blogId = this.blog.id;
          this.urlAvailable = true;
          this.urlError = '';
          this.loadMenus();
        } else if (newUserBlog?.notFound) {
          this.$router.replace('/blog/create');
        } else {
          this.blog.id = null;
          this.newMenu.blogId = null;
          this.menus = [];
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions(['fetchUserBlog']),

    async updateBlog() {
      if (this.urlError && !this.urlAvailable) {
        alert('URL을 확인해주세요.');
        return;
      }
      if (!this.blog.title) {
        alert('블로그 제목을 입력해주세요.');
        return;
      }

      if (!this.blog.id) return;

      this.isSaving = true;
      try {
        await authService.updateBlog(this.blog.id, {
          title: this.blog.title,
          description: this.blog.description,
          url: this.blog.url
        });
        await this.fetchUserBlog();
        alert('블로그가 성공적으로 수정되었습니다!');
      } catch (error) {
        alert('블로그 수정 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSaving = false;
      }
    },

    async checkUrl() {
      const originalUrl = this.userBlog?.url;
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
        }
      } catch (error) {
        this.urlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },

    async loadMenus() {
      if (!this.blog.id) return;

      this.isLoading = true;
      try {
        const response = await authService.getMenusByBlogId(this.blog.id);
        this.menus = Array.isArray(response)
            ? response.map(menu => ({
              id: menu.id,
              name: menu.name || '',
              orderIndex: menu.orderIndex ?? 0,
              parentId: menu.parentId || null,
            })).sort((a, b) => {
              // Sort by parentId first (nulls first), then by orderIndex
              const parentCompare = (a.parentId === null ? -1 : a.parentId) - (b.parentId === null ? -1 : b.parentId);
              if (parentCompare !== 0) return parentCompare;
              return a.orderIndex - b.orderIndex;
            })
            : [];
      } catch (error) {
        console.error('Failed to load menus:', error);
        this.menus = [];
      } finally {
        this.isLoading = false;
      }
    },

    async addMenu() {
      if (!this.newMenu.name) {
        alert('메뉴 이름을 입력해주세요.');
        return;
      }
      if (!this.newMenu.blogId) {
        alert('블로그 정보를 먼저 로드해주세요.');
        return;
      }
      this.isSaving = true;
      try {
        const siblingMenus = this.menus.filter(m => m.parentId === this.newMenu.parentId);
        this.newMenu.orderIndex = siblingMenus.length;

        const createdMenu = await authService.createMenu(this.newMenu);

        this.menus.push({
          id: createdMenu.id,
          name: createdMenu.name || this.newMenu.name,
          orderIndex: createdMenu.orderIndex ?? siblingMenus.length,
          parentId: createdMenu.parentId || this.newMenu.parentId,
        });

        this.newMenu.name = '';
        this.newMenu.parentId = null;

        this.menus.sort((a, b) => {
          const parentCompare = (a.parentId === null ? -1 : a.parentId) - (b.parentId === null ? -1 : b.parentId);
          if (parentCompare !== 0) return parentCompare;
          return a.orderIndex - b.orderIndex;
        });

      } catch (error) {
        console.error('Menu addition failed:', error.response?.data || error.message);
        alert('메뉴 추가 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSaving = false;
      }
    },

    onDragEnd(event) {
      const {oldIndex, newIndex} = event;
      if (oldIndex === newIndex) return;

      this.orderChanged = true;

      this.menus.forEach((menu, idx) => {
        menu.orderIndex = idx;
      });
    },

    async saveMenuOrder() {
      if (!this.orderChanged) {
        return;
      }
      this.isSaving = true;
      try {
        await authService.updateMenuOrder(this.blog.id, this.menus.map(m => ({
          id: m.id,
          orderIndex: m.orderIndex,
          parentId: m.parentId
        })));
        alert('메뉴 순서가 저장되었습니다.');
        this.orderChanged = false;
        await this.loadMenus();
      } catch (error) {
        console.error('Failed to update menu order:', error.response?.data || error.message);
        alert('메뉴 순서 저장 실패: ' + (error.response?.data?.message || error.message));
        await this.loadMenus();
      } finally {
        this.isSaving = false;
      }
    },

    getParentName(parentId) {
      if (!parentId) return '없음';
      const parent = this.menus.find(menu => menu.id === parentId);
      return parent ? parent.name : '알 수 없음';
    },
  },
});
</script>

<style scoped>
.blog-edit {
  min-height: 100vh;
  background-color: #fafafa;
  padding: 1.5rem 1rem;
}

.container {
  width: 100%;
  max-width: 700px;
  margin: 0 auto;
}

.blog-edit-content {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.08);
  padding: 2.5rem;
}

.blog-edit-header {
  text-align: center;
  margin-bottom: 2rem;
}

.blog-edit-header h1 {
  font-size: 2rem;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 0.5rem;
}

.blog-edit-header p {
  color: #666;
  font-size: 1.1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
  font-size: 0.95rem;
}

.form-input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.95rem;
  transition: all 0.2s ease;
  background-color: white;
  height: 45px; /* Consistent height */
  box-sizing: border-box; /* Include padding and border in element's total width and height */
}

select.form-input {
  appearance: none; /* For custom arrow, if needed */
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3E%3Cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 1em;
  padding-right: 2.5rem; /* Space for arrow */
}


.form-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: #aaa;
}

.form-input.input-error {
  border-color: #ef4444;
  background-color: #fff8f8;
}

.form-input.input-success {
  border-color: #10b981;
  background-color: #f0fdf4;
}

.url-input-wrapper {
  display: flex;
  align-items: center;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  height: 45px; /* Consistent height */
}

.url-prefix {
  padding: 0.8rem 0.5rem 0.8rem 1rem;
  background-color: #f5f5f5;
  color: #666;
  font-size: 0.95rem;
  white-space: nowrap;
  border-right: 1px solid #e0e0e0;
  height: 100%;
  display: flex;
  align-items: center;
}

.url-input {
  border: none;
  border-radius: 0;
  flex: 1;
  height: 100%;
}

.url-input:focus {
  box-shadow: none;
}

.url-input-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-feedback {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
}

.feedback-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  margin-right: 0.5rem;
  font-size: 12px;
}

.input-feedback.error {
  color: #ef4444;
}

.input-feedback.error .feedback-icon {
  background-color: #ef4444;
  color: white;
}

.input-feedback.success {
  color: #10b981;
}

.input-feedback.success .feedback-icon {
  background-color: #10b981;
  color: white;
}

.form-actions {
  margin-top: 2rem;
}

.submit-button {
  width: 100%;
  background-color: #000;
  color: white;
  border: none;
  padding: 0.9rem 1rem;
  border-radius: 8px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  height: 48px; /* Slightly taller button */
}

.submit-button:hover:not(:disabled) {
  background-color: #1a1a1a;
  transform: translateY(-1px);
}

.submit-button:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.button-loading {
  opacity: 0.8;
  cursor: wait;
}

.menu-management {
  margin-top: 3rem;
  border-top: 1px solid #eaeaea;
  padding-top: 2.5rem;
}

.menu-management h2 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: #1a1a1a;
  text-align: center;
}

.menu-add-section {
  margin-bottom: 2rem;
  background-color: #f9f9f9;
  padding: 1.5rem;
  border-radius: 10px;
}

.menu-add-form {
  display: flex;
  flex-wrap: wrap; /* Allow wrapping on smaller screens */
  gap: 1rem;
  align-items: flex-end;
}

.menu-add-form .form-group {
  flex-grow: 1;
  margin-bottom: 0; /* Remove default margin within the flex container */
  min-width: 150px; /* Prevent inputs from becoming too small */
}

/* Adjust flex basis for better distribution */
.menu-add-form .form-group:nth-child(1) { /* Name input */
  flex-basis: 40%;
}

.menu-add-form .form-group:nth-child(2) { /* Parent select */
  flex-basis: 35%;
}

.add-menu-button {
  background-color: #fff;
  color: #1a1a1a;
  border: 1px solid #1a1a1a;
  padding: 0 1.5rem; /* Adjust padding */
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  height: 45px; /* Match input height */
  flex-shrink: 0; /* Prevent button from shrinking */
  margin-top: auto; /* Align button nicely if wrapped */
}

.add-menu-button:hover:not(:disabled) {
  background-color: #1a1a1a;
  color: white;
}

.add-menu-button:disabled {
  background-color: #f5f5f5;
  border-color: #ccc;
  color: #999;
  cursor: not-allowed;
}

.menu-content {
  min-height: 200px;
}

.menu-list {
  background-color: #fff;
  border-radius: 10px;
  padding: 1.5rem;
  border: 1px solid #eaeaea;
}

.menu-list h3 {
  font-size: 1.2rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #1a1a1a;
}

.drag-instruction {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.draggable-container {
  margin-bottom: 1.5rem;
}

.menu-item {
  margin-bottom: 0.75rem;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.menu-item-content {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  background-color: #f9f9f9;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  cursor: move;
}

.menu-item-handle {
  color: #999;
  margin-right: 0.75rem;
  display: flex;
  align-items: center;
}

.menu-item-name {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.menu-item.sub-menu .menu-item-content {
  margin-left: 1.5rem; /* Indent sub-menus */
  background-color: #f0f7ff;
  border-color: #d1e3ff;
}

.parent-info {
  color: #666;
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.menu-actions {
  margin-top: 1.5rem;
  text-align: center;
}

.save-order-button {
  background-color: #1a1a1a;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.save-order-button:hover:not(:disabled) {
  background-color: #000;
  transform: translateY(-1px);
}

.save-order-button:disabled {
  background-color: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: #666;
  text-align: center;
  background-color: #f9f9f9;
  border-radius: 10px;
  border: 1px dashed #e0e0e0;
  min-height: 200px;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e0e0e0;
  border-top-color: #666;
  border-radius: 50%;
  margin-bottom: 1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
  opacity: 0.7;
}

.empty-state p {
  font-size: 1.1rem;
  font-weight: 500;
  margin-bottom: 0.5rem;
}

.empty-state-desc {
  color: #999;
  font-size: 0.95rem !important;
  font-weight: normal !important;
}

@media (max-width: 768px) {
  .blog-edit-content {
    padding: 1.5rem;
  }

  .blog-edit-header h1 {
    font-size: 1.75rem;
  }

  .menu-add-form {
    flex-direction: column;
    align-items: stretch; /* Stretch items to full width */
    gap: 1rem;
  }

  .menu-add-form .form-group {
    flex-basis: auto; /* Reset flex basis on smaller screens */
    min-width: 0; /* Reset min-width */
  }


  .add-menu-button {
    width: 100%;
    height: 45px; /* Match input height */
    margin-top: 0; /* Reset margin */
  }
}
</style>
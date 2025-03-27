<template>
  <div class="blog-edit">
    <NavBar/>
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
import Draggable from 'vuedraggable';
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default defineComponent({
  name: 'BlogEditView',
  components: {
    Draggable,
    NavBar
  },
  data() {
    return {
      blog: {title: '', description: '', url: '', id: null},
      isLoading: false,
      urlAvailable: false,
      urlError: '',
      menus: [],
      newMenu: {name: '', blogId: null, orderIndex: null, parentId: null},
      orderChanged: false,
    };
  },
  async created() {
    await this.loadBlog();
    await this.loadMenus();
  },
  methods: {
    async loadBlog() {
      try {
        this.isLoading = true;
        let blogId = this.$route.params.id;
        if (!blogId) {
          const user = await authService.getCurrentUser();
          const blog = await authService.getBlogByUserId(user.id);
          blogId = blog?.id;
        }
        if (!blogId) {
          this.$router.push('/blog/create');
          return;
        }
        const blog = await authService.getBlogById(blogId);
        if (!blog || typeof blog === 'string') {
          throw new Error('블로그 데이터를 불러올 수 없습니다.');
        }
        this.blog.id = blog.id;
        this.blog.title = blog.title || '';
        this.blog.description = blog.description || '';
        this.blog.url = blog.url || '';
        this.newMenu.blogId = this.blog.id;
        this.urlAvailable = true;
      } catch (error) {
        console.error('Failed to load blog:', error);
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
    },
    async updateBlog() {
      try {
        this.isLoading = true;
        await authService.updateBlog(this.blog.id, this.blog);
        alert('블로그가 수정되었습니다!');
        this.$router.push(`/blog/${this.blog.url}`);
      } catch (error) {
        alert('블로그 수정 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isLoading = false;
      }
    },
    async checkUrl() {
      this.urlError = '';
      this.urlAvailable = false;
      const urlPattern = /^[a-zA-Z0-9-]+$/;
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
      try {
        this.isLoading = true;
        const response = await authService.getMenusByBlogId(this.blog.id);
        this.menus = Array.isArray(response)
            ? response.map(menu => ({
              id: menu.id,
              name: menu.name || '',
              orderIndex: menu.orderIndex !== null && menu.orderIndex !== undefined ? menu.orderIndex : 0,
              parentId: menu.parentId || null,
            })).sort((a, b) => a.orderIndex - b.orderIndex)
            : [];
      } catch (error) {
        console.error('Failed to load menus:', error);
        this.menus = [];
      } finally {
        this.isLoading = false;
      }
    },
    async addMenu() {
      try {
        this.newMenu.orderIndex = this.menus.length;
        const createdMenu = await authService.createMenu(this.newMenu);

        this.menus.push({
          id: createdMenu.id,
          name: createdMenu.name || this.newMenu.name,
          orderIndex: createdMenu.orderIndex !== null && createdMenu.orderIndex !== undefined
              ? createdMenu.orderIndex
              : this.menus.length - 1,
          parentId: createdMenu.parentId || this.newMenu.parentId,
        });

        this.newMenu.name = '';
        this.newMenu.orderIndex = null;
        this.newMenu.parentId = null;
      } catch (error) {
        console.error('Menu addition failed:', error.response?.data || error.message);
        alert('메뉴 추가 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    onDragEnd(event) {
      const {oldIndex, newIndex} = event;
      if (oldIndex === newIndex) return;

      this.orderChanged = true;
      this.menus.forEach((menu, idx) => {
        menu.orderIndex = idx;
      });

      if (newIndex > 0) {
        const draggedMenu = this.menus[newIndex];
        const potentialParent = this.menus[newIndex - 1];
        if (draggedMenu.parentId !== potentialParent.id) {
          draggedMenu.parentId = potentialParent.id;
        }
      } else {
        this.menus[newIndex].parentId = null;
      }
    },
    async saveMenuOrder() {
      if (!this.orderChanged) {
        alert('변경된 사항이 없습니다.');
        return;
      }
      try {
        this.isLoading = true;
        await authService.updateMenuOrder(this.blog.id, this.menus);
        alert('메뉴 순서가 저장되었습니다.');
        this.orderChanged = false;
      } catch (error) {
        console.error('Failed to update menu order:', error.response?.data || error.message);
        alert('메뉴 순서 저장 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isLoading = false;
      }
    },
    getParentName(parentId) {
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
<template>
  <div class="blog-edit container mt-4">
    <h1>블로그 수정</h1>
    <form @submit.prevent="updateBlog">
      <div class="form-group">
        <label for="title">블로그 제목</label>
        <input v-model="blog.title" class="form-control" id="title" required/>
      </div>
      <div class="form-group">
        <label for="description">설명</label>
        <textarea v-model="blog.description" class="form-control" id="description"></textarea>
      </div>
      <div class="form-group">
        <label for="url">URL</label>
        <input v-model="blog.url" class="form-control" id="url" @blur="checkUrl" required/>
        <small v-if="urlError" class="text-danger">{{ urlError }}</small>
        <small v-else-if="urlAvailable" class="text-success">사용 가능한 URL입니다.</small>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="isLoading || !urlAvailable">수정하기</button>
    </form>

    <h2 class="mt-4">메뉴 관리</h2>
    <form @submit.prevent="addMenu" class="mb-3">
      <div class="form-group">
        <label for="menuName">메뉴 이름</label>
        <input v-model="newMenu.name" class="form-control" id="menuName" required/>
      </div>
      <div class="form-group">
        <label for="menuUrl">메뉴 URL</label>
        <input v-model="newMenu.url" class="form-control" id="menuUrl" @blur="checkMenuUrl" required/>
        <small v-if="menuUrlError" class="text-danger">{{ menuUrlError }}</small>
        <small v-else-if="menuUrlAvailable" class="text-success">사용 가능한 URL입니다.</small>
      </div>
      <button type="submit" class="btn btn-primary" :disabled="!menuUrlAvailable">메뉴 추가</button>
    </form>

    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="!menus.length">메뉴가 없습니다.</div>
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
          <div class="menu-item" :class="{ 'sub-menu': menu.parentId }">
            <span>{{ menu.name }} ({{ menu.url }}) - Order: {{ menu.orderIndex }}</span>
            <small v-if="menu.parentId" class="text-muted"> (Parent: {{ getParentName(menu.parentId) }})</small>
          </div>
        </template>
      </draggable>
      <button type="button" class="btn btn-success mt-2" @click="saveMenuOrder">순서 저장</button>
    </div>
  </div>
</template>

<script>
import {defineComponent} from 'vue';
import Draggable from 'vuedraggable';
import authService from '@/services/authService';

export default defineComponent({
  components: {Draggable},
  data() {
    return {
      blog: {title: '', description: '', url: '', id: null},
      isLoading: false,
      urlAvailable: false,
      urlError: '',
      menus: [],
      newMenu: {name: '', url: '', blogId: null, orderIndex: null, parentId: null},
      menuUrlAvailable: false,
      menuUrlError: '',
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
        console.log('Route params ID:', blogId);
        if (!blogId) {
          const user = await authService.getCurrentUser();
          console.log('Current user:', user);
          const blog = await authService.getBlogByUserId(user.id);
          blogId = blog?.id;
          console.log('Blog from user:', blog);
        }
        if (!blogId) {
          console.warn('No blog ID found, redirecting to create page');
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
        console.log('Loaded blog:', this.blog);
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
        console.log('Raw menu response:', response);
        this.menus = Array.isArray(response)
            ? response.map(menu => ({
              id: menu.id,
              name: menu.name || '',
              url: menu.url || '',
              orderIndex: menu.orderIndex !== null && menu.orderIndex !== undefined ? menu.orderIndex : 0,
              parentId: menu.parentId || null,
            })).sort((a, b) => a.orderIndex - b.orderIndex)
            : [];
        console.log('Processed menus:', this.menus);
      } catch (error) {
        console.error('Failed to load menus:', error);
        this.menus = [];
      } finally {
        this.isLoading = false;
      }
    },
    async checkMenuUrl() {
      this.menuUrlError = '';
      this.menuUrlAvailable = false;
      const urlPattern = /^[a-zA-Z0-9-]+$/;
      if (!urlPattern.test(this.newMenu.url)) {
        this.menuUrlError = 'URL은 영문, 숫자, 하이픈(-)만 사용할 수 있습니다.';
        return;
      }
      try {
        const available = await authService.checkMenuUrlAvailability(this.blog.id, this.newMenu.url);
        this.menuUrlAvailable = available;
        if (!available) {
          this.menuUrlError = '이미 사용 중인 URL입니다.';
        }
      } catch (error) {
        this.menuUrlError = 'URL 확인 중 오류가 발생했습니다.';
      }
    },
    async addMenu() {
      try {
        console.log('Adding menu with data:', this.newMenu);
        this.newMenu.orderIndex = this.menus.length;
        const createdMenu = await authService.createMenu(this.newMenu);
        console.log('Created menu response:', createdMenu);

        this.menus.push({
          id: createdMenu.id,
          name: createdMenu.name || this.newMenu.name,
          url: createdMenu.url || this.newMenu.url,
          blogId: this.blog.id,
          orderIndex: createdMenu.orderIndex !== null && createdMenu.orderIndex !== undefined ? createdMenu.orderIndex : this.menus.length - 1,
          parentId: createdMenu.parentId || this.newMenu.parentId,
        });

        this.newMenu.name = '';
        this.newMenu.url = '';
        this.newMenu.orderIndex = null;
        this.newMenu.parentId = null;
        this.menuUrlAvailable = false;
        console.log('Updated menus:', this.menus);
      } catch (error) {
        console.error('Menu addition failed:', error.response?.data || error.message);
        alert('메뉴 추가 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    onDragEnd(event) {
      const {oldIndex, newIndex} = event;
      console.log('Drag event:', {oldIndex, newIndex});

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
          console.log(`Set ${draggedMenu.name} as child of ${potentialParent.name}`);
        }
      } else {
        this.menus[newIndex].parentId = null;
      }

      console.log('Updated menus after drag:', this.menus);
    },
    async saveMenuOrder() {
      if (!this.orderChanged) {
        alert('변경된 사항이 없습니다.');
        return;
      }
      console.log('Saving menu order with blogId:', this.blog.id);
      console.log('Menus to save:', this.menus);
      try {
        this.isLoading = true;
        const response = await authService.updateMenuOrder(this.blog.id, this.menus);
        console.log('Save menu order response:', response);
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
      return parent ? parent.name : 'Unknown';
    },
  },
});
</script>

<style scoped>
.form-group {
  margin-bottom: 20px;
}

.menu-list {
  width: 100%;
}

.menu-item {
  padding: 10px;
  border: 1px solid #ccc;
  margin-bottom: 5px;
  background-color: #f9f9f9;
  cursor: move;
}

.sub-menu {
  margin-left: 20px;
  background-color: #f0f0f0;
}
</style>
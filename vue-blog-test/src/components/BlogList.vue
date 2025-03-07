<template>
  <div class="container mt-5">
    <h2>Blogs</h2>
    <ul class="list-group mb-3">
      <li v-for="blog in blogs" :key="blog.id" class="list-group-item">
        {{ blog.title }} ({{ blog.url }})
        <button @click="editBlog(blog)" class="btn btn-sm btn-warning ms-2">Edit</button>
        <button @click="deleteBlog(blog.id)" class="btn btn-sm btn-danger ms-2">Delete</button>
      </li>
    </ul>
    <form @submit.prevent="createOrUpdateBlog">
      <div class="mb-3"><input v-model="form.title" class="form-control" placeholder="Title" required /></div>
      <div class="mb-3"><input v-model="form.url" class="form-control" placeholder="URL" required @blur="checkUrl" /></div>
      <div class="mb-3"><input v-model="form.userId" class="form-control" placeholder="User ID" type="number" required /></div>
      <button type="submit" class="btn btn-primary">{{ editing ? 'Update' : 'Create' }}</button>
    </form>
    <p v-if="urlMessage" :class="{ 'text-danger': !urlAvailable, 'text-success': urlAvailable }">{{ urlMessage }}</p>
  </div>
</template>

<script>
import { api } from '../axios';

export default {
  data: () => ({
    blogs: [],
    form: { title: '', url: '', userId: '' },
    editing: false,
    editingId: null,
    urlAvailable: false,
    urlMessage: '',
  }),
  mounted() {
    this.fetchBlogs();
  },
  methods: {
    async fetchBlogs() {
      const response = await api.get('/blogs');
      this.blogs = response.data;
    },
    async createOrUpdateBlog() {
      try {
        if (this.editing) {
          const response = await api.put(`/blogs/${this.editingId}`, this.form, { params: { userId: this.form.userId } });
          this.blogs = this.blogs.map(b => (b.id === this.editingId ? response.data : b));
          this.resetForm();
        } else {
          const response = await api.post('/blogs', this.form, { params: { userId: this.form.userId } });
          this.blogs.push(response.data);
          this.resetForm();
        }
      } catch (error) {
        alert('블로그 처리 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async deleteBlog(blogId) {
      if (confirm('삭제하시겠습니까?')) {
        await api.delete(`/blogs/${blogId}`, { params: { userId: this.form.userId } });
        this.blogs = this.blogs.filter(b => b.id !== blogId);
      }
    },
    editBlog(blog) {
      this.form = { ...blog };
      this.editing = true;
      this.editingId = blog.id;
    },
    async checkUrl() {
      const response = await api.get('/blogs/check-url', { params: { url: this.form.url } });
      this.urlAvailable = response.data;
      this.urlMessage = this.urlAvailable ? '사용 가능한 URL입니다.' : '이미 사용 중인 URL입니다.';
    },
    resetForm() {
      this.form = { title: '', url: '', userId: '' };
      this.editing = false;
      this.editingId = null;
      this.urlMessage = '';
    },
  },
};
</script>
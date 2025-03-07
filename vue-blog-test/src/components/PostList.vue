<template>
  <div class="container mt-5">
    <h2>Posts</h2>
    <ul class="list-group mb-3">
      <li v-for="post in posts" :key="post.id" class="list-group-item">
        {{ post.title }}
        <button @click="editPost(post)" class="btn btn-sm btn-warning ms-2">Edit</button>
        <button @click="deletePost(post.id)" class="btn btn-sm btn-danger ms-2">Delete</button>
      </li>
    </ul>
    <form @submit.prevent="createOrUpdatePost" enctype="multipart/form-data">
      <div class="mb-3"><input v-model="form.title" class="form-control" placeholder="Title" required /></div>
      <div class="mb-3"><textarea v-model="form.content" class="form-control" placeholder="Content" required></textarea></div>
      <div class="mb-3"><input v-model="form.menuId" class="form-control" placeholder="Menu ID" type="number" required /></div>
      <div class="mb-3"><input v-model="form.userId" class="form-control" placeholder="User ID" type="number" required /></div>
      <div class="mb-3"><input type="file" multiple @change="onFileChange" class="form-control" /></div>
      <button type="submit" class="btn btn-primary">{{ editing ? 'Update' : 'Create' }}</button>
    </form>
  </div>
</template>

<script>
import { multipartApi } from '../axios';

export default {
  data: () => ({
    posts: [],
    form: { title: '', content: '', menuId: '', userId: '' },
    files: [],
    editing: false,
    editingId: null,
  }),
  mounted() {
    this.fetchPosts();
  },
  watch: {
    '$route.params.menuId': 'fetchPosts',
  },
  methods: {
    async fetchPosts() {
      const menuId = this.$route.params.menuId;
      if (!menuId) return;
      const response = await multipartApi.get(`/posts/menu/${menuId}`);
      this.posts = response.data;
    },
    onFileChange(event) {
      this.files = Array.from(event.target.files);
    },
    async createOrUpdatePost() {
      const formData = new FormData();
      formData.append('dto', JSON.stringify(this.form));
      this.files.forEach(file => formData.append('files', file));
      try {
        if (this.editing) {
          const response = await multipartApi.put(`/posts/${this.editingId}`, formData);
          this.posts = this.posts.map(p => (p.id === this.editingId ? response.data : p));
          this.resetForm();
        } else {
          const response = await multipartApi.post('/posts', formData);
          this.posts.push(response.data);
          this.resetForm();
        }
      } catch (error) {
        alert('게시글 처리 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async deletePost(postId) {
      if (confirm('삭제하시겠습니까?')) {
        await multipartApi.delete(`/posts/${postId}`, { params: { userId: this.form.userId } });
        this.posts = this.posts.filter(p => p.id !== postId);
      }
    },
    editPost(post) {
      this.form = { ...post };
      this.editing = true;
      this.editingId = post.id;
    },
    resetForm() {
      this.form = { title: '', content: '', menuId: '', userId: '' };
      this.files = [];
      this.editing = false;
      this.editingId = null;
    },
  },
};
</script>
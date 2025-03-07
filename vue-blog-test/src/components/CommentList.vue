<template>
  <div class="container mt-5">
    <h2>Comments</h2>
    <ul class="list-group mb-3">
      <li v-for="comment in comments" :key="comment.id" class="list-group-item">
        {{ comment.content }}
        <button @click="showReplyForm(comment.id)" class="btn btn-sm btn-secondary ms-2">Reply</button>
        <button @click="editComment(comment)" class="btn btn-sm btn-warning ms-2">Edit</button>
        <button @click="deleteComment(comment.id)" class="btn btn-sm btn-danger ms-2">Delete</button>
        <ul v-if="comment.replies?.length" class="list-group mt-2">
          <li v-for="reply in comment.replies" :key="reply.id" class="list-group-item">{{ reply.content }}</li>
        </ul>
      </li>
    </ul>
    <form @submit.prevent="createOrUpdateComment">
      <div class="mb-3"><textarea v-model="form.content" class="form-control" placeholder="Write a comment" required></textarea></div>
      <div class="mb-3"><input v-model="form.userId" class="form-control" placeholder="User ID" type="number" required /></div>
      <button type="submit" class="btn btn-primary">{{ editing ? 'Update' : 'Add' }}</button>
    </form>
    <form v-if="replyingTo" @submit.prevent="createReply" class="mt-3">
      <div class="mb-3"><textarea v-model="replyForm.content" class="form-control" placeholder="Write a reply" required></textarea></div>
      <button type="submit" class="btn btn-primary">Add Reply</button>
    </form>
  </div>
</template>

<script>
import { api } from '../axios';

export default {
  data: () => ({
    comments: [],
    form: { content: '', postId: '', userId: '' },
    replyForm: { content: '', postId: '', userId: '' },
    editing: false,
    editingId: null,
    replyingTo: null,
  }),
  mounted() {
    this.fetchComments();
  },
  watch: {
    '$route.params.postId'(newVal) {
      this.form.postId = newVal;
      this.replyForm.postId = newVal;
      this.fetchComments();
    },
  },
  methods: {
    async fetchComments() {
      const postId = this.$route.params.postId;
      if (!postId) return;
      const response = await api.get(`/comments/post/${postId}`);
      this.comments = response.data;
      for (let comment of this.comments) {
        const replies = await api.get(`/comments/${comment.id}/replies`);
        comment.replies = replies.data;
      }
    },
    async createOrUpdateComment() {
      this.form.postId = this.$route.params.postId;
      try {
        if (this.editing) {
          const response = await api.put(`/comments/${this.editingId}`, this.form);
          this.comments = this.comments.map(c => (c.id === this.editingId ? response.data : c));
          this.resetForm();
        } else {
          const response = await api.post('/comments', this.form);
          this.comments.push(response.data);
          this.resetForm();
        }
      } catch (error) {
        alert('댓글 처리 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async createReply() {
      this.replyForm.postId = this.$route.params.postId;
      try {
        const response = await api.post(`/comments/${this.replyingTo}/reply`, this.replyForm);
        const parent = this.comments.find(c => c.id === this.replyingTo);
        if (!parent.replies) parent.replies = [];
        parent.replies.push(response.data);
        this.replyForm.content = '';
        this.replyingTo = null;
      } catch (error) {
        alert('답글 추가 실패: ' + (error.response?.data?.message || error.message));
      }
    },
    async deleteComment(commentId) {
      if (confirm('삭제하시겠습니까?')) {
        await api.delete(`/comments/${commentId}`, { params: { userId: this.form.userId } });
        this.comments = this.comments.filter(c => c.id !== commentId);
      }
    },
    editComment(comment) {
      this.form = { ...comment };
      this.editing = true;
      this.editingId = comment.id;
    },
    showReplyForm(commentId) {
      this.replyingTo = commentId;
      this.replyForm.userId = this.form.userId;
    },
    resetForm() {
      this.form.content = '';
      this.editing = false;
      this.editingId = null;
    },
  },
};
</script>
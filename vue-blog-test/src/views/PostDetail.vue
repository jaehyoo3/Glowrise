<template>
  <div class="post-detail">
    <NavBar/>
    <div class="container">
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
      </div>

      <div v-else-if="post" class="post-container">
        <div class="post-header">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <span class="menu-tag">
              {{ getCurrentMenuName() }}
            </span>
            <span class="separator">•</span>
            <span class="post-date">{{ formatPostDate(post.updatedAt) }}</span>
          </div>
        </div>

        <div class="post-content">
          <!-- 이미지를 먼저 표시 -->
          <div v-if="imageFiles.length > 0" class="post-images">
            <div v-for="file in imageFiles" :key="file.id" class="content-image">
              <img :alt="file.fileName" :src="authService.getFileDownloadUrl(file.id)"/>
            </div>
          </div>

          <!-- 본문 내용 -->
          <div class="post-body" v-html="formatPostContent(post.content)"></div>

          <!-- 첨부 파일 리스트 -->
          <div v-if="post.fileIds && post.fileIds.length > 0" class="attachments">
            <div class="attachment-icon">
              <i class="fas fa-paperclip"></i>
              <span>{{ post.fileIds.length }} 첨부 파일</span>
            </div>
            <div class="attachment-list">
              <div v-for="file in files" :key="file.id" class="attachment-item">
                <div class="attachment-link">
                  <a :download="file.fileName" :href="authService.getFileDownloadUrl(file.id)">
                    {{ file.fileName }}
                  </a>
                </div>
              </div>
            </div>
          </div>

          <div class="post-actions">
            <router-link
                :to="`/${blogUrl}/${menuId}/edit/${post.id}`"
                class="edit-button"
            >
              수정
            </router-link>
            <router-link
                :to="listRoute"
                class="back-button"
            >
              목록
            </router-link>
          </div>
        </div>

        <div class="comments-section">
          <div class="comments-header">
            <h3>댓글 {{ comments.length }}</h3>
          </div>

          <div v-if="comments.length === 0" class="no-comments">
            아직 댓글이 없습니다.
          </div>

          <div v-else class="comments-list">
            <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment"
            >
              <div class="comment-content">
                <p>{{ comment.content }}</p>
                <div class="comment-meta">
                  <span class="comment-author">
                    {{ comment.authorName }}
                  </span>
                  <span class="separator">•</span>
                  <span class="comment-date">
                    {{ formatDate(comment.updatedAt) }}
                  </span>
                  <button
                      v-if="replyingTo !== comment.id"
                      class="reply-button"
                      @click="showReplyForm(comment.id)"
                  >
                    답글
                  </button>
                </div>
              </div>

              <!-- 답글 입력 폼 -->
              <div v-if="replyingTo === comment.id" class="reply-form">
                <textarea
                    v-model="newReply.content"
                    class="reply-input"
                    placeholder="답글을 입력하세요"
                ></textarea>
                <div class="reply-form-actions">
                  <button
                      :disabled="isSubmitting"
                      class="submit-reply-button"
                      @click="submitReply(comment.id)"
                  >
                    {{ isSubmitting ? '등록 중...' : '등록' }}
                  </button>
                  <button
                      class="cancel-reply-button"
                      @click="cancelReply"
                  >
                    취소
                  </button>
                </div>
              </div>

              <div v-if="comment.replies && comment.replies.length" class="replies">
                <div
                    v-for="reply in comment.replies"
                    :key="reply.id"
                    class="reply"
                >
                  <p>{{ reply.content }}</p>
                  <div class="reply-meta">
                    <span class="reply-author">
                      {{ reply.authorName }}
                    </span>
                    <span class="separator">•</span>
                    <span class="reply-date">
                      {{ formatDate(reply.updatedAt) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="comment-form">
            <textarea
                v-model="newComment.content"
                placeholder="댓글을 남겨주세요"
                class="comment-input"
            ></textarea>
            <button
                @click="submitComment"
                class="submit-comment-button"
                :disabled="isSubmitting"
            >
              {{ isSubmitting ? '등록 중...' : '댓글 등록' }}
            </button>
          </div>
        </div>
      </div>

      <div v-else class="not-found">
        <h2>게시글을 찾을 수 없습니다</h2>
        <router-link to="/" class="home-link">홈으로 돌아가기</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail {
  background-color: #f8f9fa;
  min-height: 100vh;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.post-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  padding: 2rem;
}

.post-header {
  margin-bottom: 2rem;
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 1rem;
}

.post-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  margin-bottom: 0.5rem;
}

.post-meta {
  color: #666;
  font-size: 0.9rem;
}

.menu-tag {
  background-color: #f1f3f5;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  margin-right: 0.5rem;
}

.separator {
  margin: 0 0.5rem;
}

.post-content {
  margin-bottom: 2rem;
}

.post-images {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.content-image img {
  max-width: 100%;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.post-body {
  font-size: 1.1rem;
  line-height: 1.6;
  color: #333;
}

.attachments {
  margin: 1.5rem 0;
  background-color: #f8f9fa;
  padding: 0.75rem;
  border-radius: 4px;
}

.attachment-icon {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.attachment-link a {
  color: #007bff;
  text-decoration: none;
}

.attachment-link a:hover {
  text-decoration: underline;
}

.post-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1.5rem;
}

.edit-button, .back-button {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
}

.edit-button {
  background-color: #000;
  color: white;
}

.back-button {
  background-color: #f1f3f5;
  color: #000;
}

.comments-section {
  background-color: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
  margin-top: 2rem;
}

.comments-header h3 {
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.comment {
  background-color: white;
  border-radius: 4px;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.comment-meta, .reply-meta {
  color: #666;
  font-size: 0.8rem;
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
}

.reply-button {
  margin-left: auto;
  background-color: transparent;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
  color: #666;
  cursor: pointer;
}

.reply-button:hover {
  background-color: #f1f3f5;
}

.reply-form {
  margin-top: 1rem;
  background-color: #f1f3f5;
  padding: 1rem;
  border-radius: 4px;
}

.reply-input {
  width: 100%;
  min-height: 80px;
  padding: 0.75rem;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  resize: vertical;
  margin-bottom: 0.75rem;
}

.reply-form-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.submit-reply-button,
.cancel-reply-button {
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
}

.submit-reply-button {
  background-color: #000;
  color: white;
}

.cancel-reply-button {
  background-color: #e5e5e5;
  color: #333;
}

.replies {
  margin-top: 1rem;
  border-left: 2px solid #e5e5e5;
  padding-left: 1rem;
}

.reply {
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 0.75rem;
  margin-bottom: 0.75rem;
}

.comment-form {
  margin-top: 1.5rem;
}

.comment-input {
  width: 100%;
  min-height: 100px;
  padding: 1rem;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  resize: vertical;
  margin-bottom: 1rem;
}

.submit-comment-button {
  background-color: #000;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
}

.loading-state, .not-found {
  text-align: center;
  padding: 2rem;
}

@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .post-title {
    font-size: 2rem;
  }
}
</style>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'PostDetail',
  components: { NavBar },
  props: {
    blogUrl: String,
    menuId: String,
    postId: String,
  },
  data() {
    return {
      post: null,
      allMenus: [],
      comments: [],
      files: [], // 파일 정보 저장
      newComment: {content: '', postId: null, userId: null, authorName: ''},
      newReply: {content: '', postId: null, userId: null, authorName: ''},
      replyingTo: null,
      isLoading: true,
      isSubmitting: false,
      currentUser: null,
      showEmailDropdown: null,
      blog: null,
      authService, // authService를 직접 접근하기 위해 추가
    };
  },
  computed: {
    listRoute() {
      if (this.$route.query.fromAll === 'true') {
        return `/${this.blogUrl}`;
      }
      return `/${this.blogUrl}/${this.menuId}`;
    },
    // 이미지 파일만 필터링
    imageFiles() {
      return this.files.filter(file => this.isImage(file.contentType));
    },
  },
  watch: {
    '$route.query.refresh': {
      immediate: true,
      handler() {
        this.loadPostAndMenus();
      },
    },
  },
  async created() {
    await this.loadPostAndMenus();
  },
  methods: {
    async loadPostAndMenus() {
      try {
        this.isLoading = true;
        const postResponse = await authService.getPostById(this.postId);
        this.post = postResponse;

        if (!this.post.menuId) {
          this.post.menuId = Number(this.menuId);
        }

        const blogResponse = await authService.getBlogByUrl(this.blogUrl);
        this.blog = blogResponse;

        const menusResponse = await authService.getMenusByBlogId(this.blog.id);
        this.allMenus = menusResponse;

        const commentsResponse = await authService.getCommentsByPostId(this.postId);
        this.comments = this.processComments(commentsResponse);

        this.currentUser = await authService.getCurrentUser();
        this.newComment.postId = this.postId;
        this.newComment.userId = this.currentUser.id;
        this.newComment.authorName = this.currentUser.nickName || this.currentUser.username;
        this.newReply.postId = this.postId;
        this.newReply.userId = this.currentUser.id;
        this.newReply.authorName = this.newComment.authorName;

        // 파일 정보 로드
        if (this.post.fileIds && this.post.fileIds.length > 0) {
          await this.loadFiles();
        }

        this.processMenus();
      } catch (error) {
        this.post = null;
        this.blog = null;
        this.allMenus = [];
        this.comments = [];
        this.files = [];
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
    },

    async loadFiles() {
      try {
        const filePromises = this.post.fileIds.map(fileId => authService.getFileById(fileId));
        this.files = await Promise.all(filePromises);
      } catch (error) {
        console.error('파일 로드 실패:', error);
        this.files = [];
      }
    },

    isImage(contentType) {
      return contentType && contentType.startsWith('image/');
    },

    formatPostDate(date) {
      return this.formatDate(date);
    },

    formatPostContent(content) {
      return content.replace(/\n/g, '<br>');
    },

    getCurrentMenuName() {
      const currentMenu = this.allMenus.find(menu => menu.id === this.post.menuId);
      return currentMenu ? currentMenu.name : '분류 없음';
    },

    processMenus() {
      const menuMap = new Map();
      this.allMenus.forEach(menu => {
        menu.subMenus = [];
        menuMap.set(menu.id, menu);
      });
      this.allMenus.forEach(menu => {
        if (menu.parentId) {
          const parent = menuMap.get(menu.parentId);
          if (parent) parent.subMenus.push(menu);
        }
      });
      this.menus = this.allMenus.filter(menu => !menu.parentId);
    },

    processComments(comments) {
      const filteredComments = comments.filter(comment => !comment.deleted);
      filteredComments.forEach(comment => {
        comment.authorName = comment.authorName || 'Unknown';
        comment.email = comment.email || 'N/A';
        if (comment.replies && comment.replies.length > 0) {
          comment.replies = comment.replies.filter(reply => !reply.deleted);
          comment.replies.forEach(reply => {
            reply.authorName = reply.authorName || 'Unknown';
            reply.email = reply.email || 'N/A';
          });
        } else {
          comment.replies = [];
        }
      });
      return filteredComments;
    },

    async submitComment() {
      if (!this.newComment.content.trim()) {
        alert('댓글 내용을 입력하세요.');
        return;
      }
      this.isSubmitting = true;
      try {
        const createdComment = await authService.createComment(this.newComment);
        this.comments.push({
          ...createdComment,
          replies: [],
          authorName: this.currentUser.nickName || this.currentUser.username,
          email: this.currentUser.email || 'N/A',
        });
        this.newComment.content = '';
      } catch (error) {
        alert('댓글 작성 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmitting = false;
      }
    },

    showReplyForm(commentId) {
      this.replyingTo = commentId;
      this.newReply.content = '';
    },

    async submitReply(parentId) {
      if (!this.newReply.content.trim()) {
        alert('답글 내용을 입력하세요.');
        return;
      }
      this.isSubmitting = true;
      try {
        const createdReply = await authService.createReply(parentId, this.newReply);
        const parentComment = this.comments.find(c => c.id === parentId);
        if (parentComment) {
          parentComment.replies.push({
            ...createdReply,
            authorName: this.currentUser.nickName || this.currentUser.username,
            email: this.currentUser.email || 'N/A',
          });
        }
        this.replyingTo = null;
        this.newReply.content = '';
      } catch (error) {
        alert('답글 작성 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmitting = false;
      }
    },

    cancelReply() {
      this.replyingTo = null;
      this.newReply.content = '';
    },

    toggleEmailDropdown(commentId) {
      this.showEmailDropdown = this.showEmailDropdown === commentId ? null : commentId;
    },

    getParentName(parentId) {
      const parent = this.allMenus.find(menu => menu.id === parentId);
      return parent ? parent.name : '';
    },

    isParentMenu(menuId) {
      return this.allMenus.some(menu => menu.parentId === menuId);
    },

    formatDate(dateString) {
      if (!dateString) return '날짜 없음';
      const date = new Date(dateString);
      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();
      const isThisYear = date.getFullYear() === now.getFullYear();

      if (isToday) {
        return date.toLocaleTimeString('ko-KR', {
          hour: '2-digit',
          minute: '2-digit',
          hour12: false,
        });
      }

      const padZero = (num) => String(num).padStart(2, '0');
      const year = date.getFullYear();
      const month = padZero(date.getMonth() + 1);
      const day = padZero(date.getDate());

      if (isThisYear) {
        return `${month}.${day}`;
      }

      return `${year}.${month}.${day}`;
    },
  },
};
</script>
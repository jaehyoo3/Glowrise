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
      newComment: {content: '', postId: null, userId: null, authorName: ''},
      newReply: {content: '', postId: null, userId: null, authorName: ''},
      replyingTo: null,
      isLoading: true,
      isSubmitting: false,
      currentUser: null,
      showEmailDropdown: null,
    };
  },
  watch: {
    '$route.query.refresh': {
      immediate: true,
      handler() {
        this.loadPostAndMenus();
      }
    }
  },
  async created() {
    console.log('PostDetail created with route params:', this.$route.params);
    console.log('Props received - blogUrl:', this.blogUrl, 'menuId:', this.menuId, 'postId:', this.postId);
    await this.loadPostAndMenus();
  },
  methods: {
    async loadPostAndMenus() {
      try {
        this.isLoading = true;
        console.log('Starting to fetch post with blogUrl:', this.blogUrl, 'menuId:', this.menuId, 'postId:', this.postId);

        const postResponse = await authService.getPostById(this.postId);
        this.post = postResponse;

        if (!this.post.menuId) {
          console.warn('post.menuId is null, using URL menuId as fallback:', this.menuId);
          this.post.menuId = Number(this.menuId);
        } else if (this.post.menuId !== Number(this.menuId)) {
          console.warn('menuId mismatch! Post:', this.post.menuId, 'URL:', this.menuId);
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

        this.processMenus();
      } catch (error) {
        console.error('게시글 또는 메뉴 로드 실패:', error.message);
        this.post = null;
        this.blog = null;
        this.allMenus = [];
        this.comments = [];
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
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
      console.log('Raw comments from API:', JSON.stringify(comments, null, 2));
      comments.forEach(comment => {
        comment.authorName = comment.authorName || 'Unknown';
        comment.email = comment.email || 'N/A';
        if (comment.replies && comment.replies.length > 0) {
          comment.replies.forEach(reply => {
            reply.authorName = reply.authorName || 'Unknown';
            reply.email = reply.email || 'N/A';
          });
        }
      });
      const filteredComments = comments.filter(comment => !comment.deleted);
      console.log('Processed comments:', JSON.stringify(filteredComments, null, 2));
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
        console.log('Created comment:', JSON.stringify(createdComment));
        this.comments.push({
          ...createdComment,
          replies: [],
          authorName: this.currentUser.nickName || this.currentUser.username,
          email: this.currentUser.email || 'N/A'
        });
        this.newComment.content = '';
      } catch (error) {
        console.error('댓글 작성 실패:', error);
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
        console.log('Created reply:', JSON.stringify(createdReply));
        const parentComment = this.comments.find(c => c.id === parentId);
        if (parentComment) {
          parentComment.replies.push({
            ...createdReply,
            authorName: this.currentUser.nickName || this.currentUser.username,
            email: this.currentUser.email || 'N/A'
          });
        }
        this.replyingTo = null;
        this.newReply.content = '';
      } catch (error) {
        console.error('답글 작성 실패:', error);
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
  },
};
</script>

<template>
  <div class="post-detail container mt-4">
    <NavBar/>
    <h1>게시글 상세</h1>
    <div v-if="isLoading">로딩 중...</div>
    <div v-else-if="post">
      <div class="post-content">
        <h2>{{ post.title }}</h2>
        <p>{{ post.content }}</p>
        <div v-if="post.fileIds && post.fileIds.length > 0">
          <p>첨부 파일: {{ post.fileIds.length }}개</p>
        </div>
        <div class="form-group">
          <label for="menuSelect">현재 메뉴</label>
          <select v-model="post.menuId" class="form-control" id="menuSelect" disabled>
            <option v-for="menu in allMenus" :key="menu.id" :value="menu.id" :disabled="isParentMenu(menu.id)">
              {{ menu.name }} {{ menu.parentId ? `(하위: ${getParentName(menu.parentId)})` : '' }}
            </option>
          </select>
        </div>
        <router-link :to="`/${blogUrl}/${menuId}/edit/${post.id}`" class="btn btn-primary mt-2">수정</router-link>
        <router-link :to="`/${blogUrl}/${menuId}`" class="btn btn-secondary mt-2 ml-2">목록으로</router-link>
      </div>

      <div class="comments-section mt-4">
        <h3>댓글</h3>
        <div v-if="comments.length === 0">댓글이 없습니다.</div>
        <div v-else>
          <div v-for="comment in comments" :key="comment.id" class="comment" :class="{ 'reply': comment.parentId }">
            <p>{{ comment.content }}</p>
            <small>
              작성자:
              <span class="author-name" @click="toggleEmailDropdown(comment.id)">
                {{ comment.authorName }}
              </span>
              <div v-if="showEmailDropdown === comment.id" class="email-dropdown">
                {{ comment.email }}
              </div>
              | {{ comment.createdDate }}
            </small>
            <button @click="showReplyForm(comment.id)" class="btn btn-sm btn-link">답글</button>
            <div v-if="replyingTo === comment.id" class="reply-form mt-2">
              <textarea v-model="newReply.content" placeholder="답글을 입력하세요" class="form-control"></textarea>
              <button @click="submitReply(comment.id)" class="btn btn-primary mt-2">답글 작성</button>
              <button @click="cancelReply" class="btn btn-secondary mt-2 ml-2">취소</button>
            </div>
            <div v-if="comment.replies && comment.replies.length > 0" class="replies">
              <div v-for="reply in comment.replies" :key="reply.id" class="comment reply">
                <p>{{ reply.content }}</p>
                <small>
                  작성자:
                  <span class="author-name" @click="toggleEmailDropdown(reply.id)">
                    {{ reply.authorName }}
                  </span>
                  <div v-if="showEmailDropdown === reply.id" class="email-dropdown">
                    {{ reply.email }}
                  </div>
                  | {{ reply.createdDate }}
                </small>
              </div>
            </div>
          </div>
        </div>

        <div class="comment-form mt-3">
          <textarea v-model="newComment.content" placeholder="댓글을 입력하세요" class="form-control"></textarea>
          <button @click="submitComment" class="btn btn-primary mt-2" :disabled="isSubmitting">댓글 작성</button>
        </div>
      </div>
    </div>
    <div v-else>
      <h1>게시글을 찾을 수 없습니다.</h1>
      <router-link :to="`/`" class="btn btn-primary">홈으로 돌아가기</router-link>
    </div>
  </div>
</template>

<style scoped>
.post-detail {
  max-width: 800px;
  margin: 0 auto;
}

.post-content {
  margin-top: 20px;
}

.comments-section {
  margin-top: 30px;
}

.comment {
  border-bottom: 1px solid #ddd;
  padding: 10px 0;
}

.reply {
  margin-left: 20px;
  border-left: 2px solid #007bff;
  padding-left: 10px;
}

.reply-form {
  margin-left: 20px;
}

.form-group, .comment-form {
  margin-bottom: 20px;
}

.form-control {
  width: 100%;
}

.btn {
  margin-right: 10px;
}

.btn-sm {
  font-size: 0.8rem;
}

.ml-2 {
  margin-left: 10px;
}

#menuSelect option:disabled {
  color: #888;
  font-style: italic;
}

.author-name {
  cursor: pointer;
  color: #007bff;
  position: relative;
}

.author-name:hover {
  text-decoration: underline;
}

.email-dropdown {
  position: absolute;
  background-color: #fff;
  border: 1px solid #ddd;
  padding: 5px 10px;
  border-radius: 4px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  z-index: 10;
  margin-top: 5px;
}
</style>
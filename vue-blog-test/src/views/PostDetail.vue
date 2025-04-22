<template>
  <div class="post-detail">
    <div class="container">
      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <span>게시글을 불러오는 중입니다...</span>
      </div>

      <div v-else-if="post" class="post-container">
        <div class="post-header">
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <span class="menu-tag">{{ getCurrentMenuName() }}</span>
            <span class="separator">•</span>
            <span class="post-date">{{ formatPostDate(post.updatedAt) }}</span>
          </div>
        </div>

        <div class="post-content">
          <div class="post-body ql-editor ql-snow" @click="handleContentClick" v-html="post.content"></div>

          <div class="post-actions">
            <router-link v-if="canEditPost" :to="`/${blogUrl}/${post.menuId}/edit/${post.id}`" class="edit-button">수정
            </router-link>
            <button v-if="canEditPost" class="delete-button" @click="deletePost">삭제</button>
            <router-link :to="listRoute" class="back-button">목록</router-link>
          </div>
        </div>

        <div class="comments-section">
          <div class="comments-header"><h3>댓글 {{ comments.length }}</h3></div>
          <div v-if="comments.length === 0" class="no-comments">아직 댓글이 없습니다. 첫 댓글을 남겨주세요!</div>
          <div v-else class="comments-list">
            <div v-for="comment in comments" :key="comment.id" class="comment">
              <div class="comment-content">
                <p v-html="formatCommentContent(comment.content)"></p>
                <div class="comment-meta">
                  <span class="comment-author"><i class="fas fa-user"></i> {{ comment.authorName || '익명' }}</span>
                  <span class="separator">•</span>
                  <span class="comment-date"><i class="far fa-clock"></i> {{ formatDate(comment.updatedAt) }}</span>
                  <button v-if="isLoggedIn && userId !== comment.userId && replyingTo !== comment.id"
                          class="reply-button" @click="showReplyForm(comment.id)">답글
                  </button>
                  <div v-if="isLoggedIn && userId === comment.userId" class="comment-actions">
                    <button class="action-button edit-comment-btn" @click="editComment(comment)">수정</button>
                    <button class="action-button delete-comment-btn" @click="deleteComment(comment)">삭제</button>
                  </div>
                </div>
              </div>
              <div v-if="replyingTo === comment.id" class="reply-form">
                <textarea v-model="newReply.content" class="reply-input" placeholder="답글을 입력하세요" rows="3"></textarea>
                <div class="reply-form-actions">
                  <button :disabled="isSubmittingReply" class="submit-reply-button" @click="submitReply(comment.id)">
                    {{ isSubmittingReply ? '등록 중...' : '답글 등록' }}
                  </button>
                  <button class="cancel-reply-button" @click="cancelReply">취소</button>
                </div>
              </div>
              <div v-if="comment.replies && comment.replies.length" class="replies">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply">
                  <p v-html="formatCommentContent(reply.content)"></p>
                  <div class="reply-meta">
                    <span class="reply-author"><i class="fas fa-user"></i> {{ reply.authorName || '익명' }}</span>
                    <span class="separator">•</span>
                    <span class="reply-date"><i class="far fa-clock"></i> {{ formatDate(reply.updatedAt) }}</span>
                    <div v-if="isLoggedIn && userId === reply.userId" class="comment-actions">
                      <button class="action-button edit-comment-btn" @click="editComment(reply, comment.id)">수정</button>
                      <button class="action-button delete-comment-btn" @click="deleteComment(reply)">삭제</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="comment-form">
            <textarea v-model="newComment.content" :disabled="!isLoggedIn"
                      :placeholder="isLoggedIn ? '댓글을 남겨주세요' : '댓글을 작성하려면 로그인이 필요합니다.'" class="comment-input" rows="4"></textarea>
            <button :disabled="isSubmittingComment || !isLoggedIn || !newComment.content.trim()" class="submit-comment-button"
                    @click="submitComment">
              {{ isSubmittingComment ? '등록 중...' : '댓글 등록' }}
            </button>
            <p v-if="!isLoggedIn" class="login-prompt">
              <router-link to="/login">로그인</router-link>
              후 댓글을 작성할 수 있습니다.
            </p>
          </div>
        </div>
      </div>

      <div v-else-if="!isLoading" class="not-found">
        <h2>게시글을 찾을 수 없습니다</h2>
        <p>요청하신 게시글이 존재하지 않거나 삭제되었을 수 있습니다.</p>
        <router-link :to="`/${blogUrl}`" class="home-link">블로그 홈으로 돌아가기</router-link>
      </div>

      <vue-easy-lightbox
          :imgs="lightboxImgs"
          :index="lightboxIndex"
          :visible="lightboxVisible"
          @hide="hideLightbox"
      ></vue-easy-lightbox>
    </div>
  </div>
</template>

<script>
import {mapGetters} from 'vuex'; // Vuex 헬퍼 함수 import
import authService from '@/services/authService'; // API 호출용 서비스 import
import '@vueup/vue-quill/dist/vue-quill.snow.css'; // Quill 콘텐츠 스타일링
import VueEasyLightbox from 'vue-easy-lightbox'; // 라이트박스 컴포넌트

export default {
  name: 'PostDetail',
  components: {VueEasyLightbox}, // 라이트박스 컴포넌트 등록
  props: {
    blogUrl: String,
    menuId: String,
    postId: String,
  },
  data() {
    return {
      post: null,          // 현재 게시글 데이터
      allMenus: [],      // 메뉴 목록 (이름 조회용)
      comments: [],      // 댓글 목록
      newComment: {content: '', postId: null, userId: null, authorName: ''}, // 새 댓글 데이터
      newReply: {content: '', postId: null, userId: null, authorName: ''}, // 새 답글 데이터
      replyingTo: null, // 현재 답글 달 대상 댓글 ID
      isLoading: true,
      isSubmittingComment: false,
      isSubmittingReply: false,
      blog: null, // 현재 게시글이 속한 블로그 정보
      authService, // 템플릿에서 authService 함수 직접 사용 (선택적)
      lightboxVisible: false, // 라이트박스 표시 여부
      lightboxIndex: 0,     // 라이트박스 이미지 인덱스
      lightboxImgs: [],     // 라이트박스 이미지 URL 배열
    };
  },
  computed: {
    // --- Vuex Getters 매핑 ---
    // 'mapState'는 사용하지 않으므로 제거
    ...mapGetters(['isLoggedIn', 'userId', 'username', 'nickName']),
    // ------------------------

    // 목록 경로 계산
    listRoute() {
      if (this.$route.query.fromAll === 'true') return `/${this.blogUrl}`;
      return `/${this.blogUrl}/${this.post?.menuId || this.menuId}`;
    },

    // 수정 권한 계산 (Vuex getter 사용)
    canEditPost() {
      return this.isLoggedIn && this.post && this.userId === this.post.userId;
    }
  },
  watch: {
    // postId 변경 감지
    postId: {
      immediate: true,
      handler(newPostId) {
        if (newPostId) {
          console.log(`PostDetail Watcher: postId 변경됨 -> ${newPostId}, 데이터 로드 시작.`);
          this.loadPostData();
        }
      }
    },
    // 라우트 쿼리 변경 감지
    '$route.query.refresh': {
      handler() {
        this.loadPostData();
      }
    },
    // userId 변경 감지 (댓글 작성자 정보 업데이트)
    userId: {
      immediate: true,
      handler(newUserId) {
        if (newUserId) {
          const authorDisplayName = this.nickName || this.username;
          this.newComment.userId = newUserId;
          this.newComment.authorName = authorDisplayName;
          this.newReply.userId = newUserId;
          this.newReply.authorName = authorDisplayName;
        } else {
          this.newComment.userId = null;
          this.newComment.authorName = '';
          this.newReply.userId = null;
          this.newReply.authorName = '';
        }
      }
    }
  },
  methods: {
    // 게시글 관련 모든 데이터 로드
    async loadPostData() {
      if (!this.postId || isNaN(Number(this.postId))) {
        console.error("PostDetail: 유효하지 않은 postId:", this.postId);
        this.isLoading = false;
        this.post = null;
        return;
      }
      console.log(`PostDetail: postId ${this.postId} 데이터 로드 시작...`);
      this.isLoading = true;
      try {
        // 게시글, 블로그, 메뉴, 댓글 정보 로드
        this.post = await authService.getPostById(this.postId);
        if (!this.post) throw new Error('게시글 정보 없음 (404 등)');
        if (!this.post.menuId && this.menuId) this.post.menuId = Number(this.menuId);
        this.newComment.postId = this.postId;
        this.newReply.postId = this.postId;

        this.blog = await authService.getBlogByUrl(this.blogUrl);
        if (!this.blog) throw new Error('블로그 정보 없음');

        const menusResponse = await authService.getMenusByBlogId(this.blog.id);
        const hierarchicalMenus = this.buildMenuHierarchy(menusResponse);
        this.allMenus = this.flattenMenusForNameLookup(hierarchicalMenus);

        const commentsResponse = await authService.getCommentsByPostId(this.postId);
        this.comments = this.processComments(commentsResponse);

      } catch (error) {
        console.error('PostDetail: 게시글 데이터 로드 실패:', error);
        this.post = null;
        this.blog = null;
        this.allMenus = [];
        this.comments = [];
        if (error.response?.status === 404) {
          console.log('PostDetail: 게시글 또는 관련 정보 404 Not Found');
        } else {
          alert('게시글을 불러오는 중 오류가 발생했습니다.');
        }
      } finally {
        this.isLoading = false;
        console.log('PostDetail: 데이터 로드 프로세스 종료.');
      }
    },

    // 메뉴 평탄화 (이름 조회용)
    flattenMenusForNameLookup(hierarchicalMenus) {
      const result = [];
      const flatten = (menuList, depth = 0) => {
        if (!Array.isArray(menuList)) return;
        menuList.forEach(menu => {
          if (!menu) return;
          const prefix = depth > 0 ? '\u00A0'.repeat(depth * 2) + '└ ' : '';
          result.push({...menu, name: prefix + (menu.name || '')});
          if (menu.subMenus && menu.subMenus.length > 0) {
            flatten(menu.subMenus, depth + 1);
          }
        });
      };
      flatten(hierarchicalMenus);
      return result;
    },

    // 메뉴 평탄 DTO -> 계층 구조 변환
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

    // 댓글 처리 (삭제된 댓글 필터링 등)
    processComments(comments) { // 'comments' 파라미터 사용됨
      if (!comments) return [];
      const filteredComments = comments.filter(comment => !comment.deleted);
      filteredComments.forEach(comment => {
        comment.authorName = comment.authorName || '익명'; // 기본값 설정
        if (comment.replies?.length > 0) {
          // 답글도 삭제된 것 필터링하고 이름 기본값 설정
          comment.replies = comment.replies.filter(reply => !reply.deleted);
          comment.replies.forEach(reply => {
            reply.authorName = reply.authorName || '익명';
          });
        } else {
          comment.replies = []; // 답글 없으면 빈 배열 확인
        }
      });
      return filteredComments; // 처리된 댓글 목록 반환
    },

    // 현재 메뉴 이름 가져오기
    getCurrentMenuName() { // 파라미터 없음
      // this.allMenus 와 this.post.menuId 사용
      const currentMenu = this.allMenus.find(menu => menu.id === Number(this.post?.menuId));
      // 이름에서 prefix 제거 (예: '└ ' 또는 &nbsp;)
      return currentMenu ? currentMenu.name.replace(/^(\s|\u00A0|&nbsp;)*└\s*/, '') : '미분류';
    },

    // 댓글/답글 작성 (Vuex getter 사용)
    // --- 댓글/답글 작성 (Vuex getter 사용) ---
    async submitComment() {
      // 로그인 상태 확인
      if (!this.isLoggedIn) {
        alert('로그인이 필요합니다.');
        // 여기에 로그인 모달을 열거나 로그인 페이지로 이동하는 로직 추가 가능
        // 예: this.$emit('open-login-modal'); 또는 this.$router.push('/login');
        return;
      }
      // 내용 확인
      if (!this.newComment.content.trim()) {
        alert('댓글 내용을 입력해주세요.');
        return;
      }
      // 사용자 ID 확인 (watch에서 설정됨)
      if (!this.newComment.userId) {
        alert('사용자 정보를 확인할 수 없습니다. 페이지를 새로고침하거나 다시 로그인해주세요.');
        return;
      }

      this.isSubmittingComment = true; // 제출 시작 상태
      try {
        // authService.createComment 호출 (API 요청)
        // newComment 객체에는 postId, userId, authorName, content가 포함됨
        const createdComment = await authService.createComment(this.newComment);

        // 성공 시 UI에 즉시 반영
        this.comments.push({
          ...createdComment, // 백엔드에서 받은 댓글 정보 (id, createdAt 등 포함)
          replies: [], // 새 댓글이므로 답글은 없음
          authorName: this.newComment.authorName, // 스토어에서 가져온 작성자 이름
          userId: this.newComment.userId,         // 스토어에서 가져온 작성자 ID
          updatedAt: new Date().toISOString()      // 즉시 표시용 업데이트 시간 (백엔드 값과 다를 수 있음)
        });
        this.newComment.content = ''; // 댓글 입력창 비우기
        console.log("PostDetail: 댓글 작성 성공", createdComment);

      } catch (error) {
        console.error("PostDetail: 댓글 작성 실패:", error);
        alert('댓글 작성에 실패했습니다: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmittingComment = false; // 제출 완료 상태
      }
    },

    showReplyForm(commentId) {
      // 답글 폼 표시
      // 로그인 상태 확인
      if (!this.isLoggedIn) {
        alert('로그인이 필요합니다.');
        return;
      }
      this.replyingTo = commentId; // 어떤 댓글에 답글을 다는지 ID 저장
      this.newReply.content = ''; // 답글 입력창 초기화
      // 필요하다면 해당 위치로 스크롤하는 로직 추가 가능
    },

    async submitReply(parentId) {
      // 답글 제출
      // 로그인 상태 확인
      if (!this.isLoggedIn) {
        alert('로그인이 필요합니다.');
        return;
      }
      // 내용 확인
      if (!this.newReply.content.trim()) {
        alert('답글 내용을 입력해주세요.');
        return;
      }
      // 사용자 ID 확인 (watch에서 설정됨)
      if (!this.newReply.userId) {
        alert('사용자 정보를 확인할 수 없습니다.');
        return;
      }
      // parentId 유효성 확인 (필요시)
      if (!parentId) {
        alert('답글을 달 대상 댓글을 찾을 수 없습니다.');
        return;
      }


      this.isSubmittingReply = true; // 제출 시작 상태
      try {
        // authService.createReply 호출 (API 요청)
        // newReply 객체에는 postId, userId, authorName, content가 포함됨
        const createdReply = await authService.createReply(parentId, this.newReply);

        // 성공 시 UI 업데이트
        const parentComment = this.comments.find(c => c.id === parentId);
        if (parentComment) {
          // 부모 댓글의 replies 배열이 없으면 초기화
          if (!Array.isArray(parentComment.replies)) {
            parentComment.replies = [];
          }
          // replies 배열에 새 답글 추가
          parentComment.replies.push({
            ...createdReply, // 백엔드에서 받은 답글 정보
            authorName: this.newReply.authorName, // 스토어에서 가져온 이름
            userId: this.newReply.userId,         // 스토어에서 가져온 ID
            updatedAt: new Date().toISOString()    // 즉시 표시용 시간
          });
          console.log("PostDetail: 답글 작성 성공", createdReply);
        } else {
          console.warn("PostDetail: 답글을 추가할 부모 댓글을 찾지 못했습니다.", parentId);
          // 이 경우 댓글 목록을 새로고침하는 것이 좋을 수 있음
          // await this.loadPostData(); // 예: 전체 데이터 리로드
        }

        this.replyingTo = null; // 답글 폼 숨기기
        this.newReply.content = ''; // 답글 입력창 비우기

      } catch (error) {
        console.error("PostDetail: 답글 작성 실패:", error);
        alert('답글 작성에 실패했습니다: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isSubmittingReply = false; // 제출 완료 상태
      }
    },

    cancelReply() {
      // 답글 작성 취소
      this.replyingTo = null; // 답글 대상 ID 초기화 (폼 숨김)
      this.newReply.content = ''; // 답글 입력 내용 초기화
    },

    // 댓글/답글 삭제 (Vuex getter 사용)
    async deleteComment(comment) { // 삭제할 comment 객체를 인자로 받음
      if (!comment || !comment.id) {
        console.error("PostDetail: 삭제할 댓글 정보가 유효하지 않습니다.");
        return;
      }
      // 로그인 상태 확인
      if (!this.isLoggedIn) {
        alert('로그인이 필요합니다.');
        return;
      }
      // 작성자 본인 확인 (스토어 userId와 댓글 userId 비교)
      if (this.userId !== comment.userId) {
        alert('삭제 권한이 없습니다.');
        return;
      }

      // 사용자에게 삭제 재확인
      if (confirm("댓글(답글)을 정말 삭제하시겠습니까?")) {
        try {
          // authService.deleteComment 호출 (API 요청)
          await authService.deleteComment(comment.id);

          // 성공 시 UI에서 해당 댓글/답글 제거
          let deleted = false;
          this.comments = this.comments.reduce((acc, c) => {
            if (c.id === comment.id) { // 최상위 댓글 확인
              deleted = true;
              return acc; // 현재 댓글 제외하고 누적
            }
            if (c.replies) { // 답글 확인
              c.replies = c.replies.filter(r => {
                if (r.id === comment.id) { // 답글 ID 일치
                  deleted = true;
                  return false; // 해당 답글 제외
                }
                return true; // 다른 답글 유지
              });
            }
            acc.push(c); // 댓글 유지 (답글이 변경되었을 수 있음)
            return acc;
          }, []); // 초기 누적값은 빈 배열

          if (deleted) {
            alert('삭제되었습니다.');
            console.log("PostDetail: 댓글/답글 삭제 완료 (ID:", comment.id, ")");
          } else {
            console.warn('PostDetail: UI에서 삭제할 댓글/답글을 찾지 못했습니다:', comment.id);
            // 만일을 위해 댓글 목록 새로고침 고려
            // await this.loadPostData();
          }
        } catch (error) {
          console.error("PostDetail: 댓글/답글 삭제 실패:", error);
          alert('삭제 실패: ' + (error.response?.data?.message || error.message));
        }
      }
    },

    // 게시글 삭제 (canEditPost computed 속성 사용)
    async deletePost() {
      // 수정/삭제 권한 확인 (computed 속성 사용)
      if (!this.canEditPost) {
        alert('게시글을 삭제할 권한이 없습니다.');
        return;
      }
      // 사용자에게 삭제 재확인
      if (confirm("게시글을 정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) {
        try {
          // authService.deletePost 호출 (API 요청)
          // deletePost(postId, userId) 형식으로 userId 전달 (API 스펙 확인)
          if (!this.postId || !this.userId) {
            throw new Error("게시글 ID 또는 사용자 ID가 없습니다.");
          }
          await authService.deletePost(this.postId, this.userId); // API 호출
          alert('게시글이 삭제되었습니다.');
          // 삭제 성공 후 목록 페이지로 이동
          this.$router.push(this.listRoute); // listRoute computed 속성 사용
        } catch (error) {
          console.error("PostDetail: 게시글 삭제 실패:", error);
          alert('게시글 삭제 실패: ' + (error.response?.data?.message || error.message));
        }
      }
    },

    // --- 유틸리티 메서드 ---
    // 게시글 내용 클릭 시 이미지 라이트박스 표시
    handleContentClick(event) { // 'event' 파라미터 사용됨
      if (event.target.tagName === 'IMG') { // IMG 태그 클릭 시
        const imageUrl = event.target.src;
        if (imageUrl) { // src 속성이 있는지 확인
          console.log('Clicked image URL:', imageUrl);
          this.lightboxImgs = [imageUrl]; // 클릭된 이미지만 배열에 넣음
          this.lightboxIndex = 0;         // 첫 번째 이미지
          this.lightboxVisible = true;    // 라이트박스 표시
        }
      }
    },

    // 라이트박스 닫기
    hideLightbox() { // 파라미터 없음
      this.lightboxVisible = false;
    },

    // 게시글 날짜 포맷 (상대 시간 + 절대 날짜)
    formatDate(dateString) { // 'dateString' 파라미터 사용됨
      if (!dateString) return '날짜 없음';
      try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return '날짜 오류';

        const now = new Date();
        const diffSeconds = Math.floor((now - date) / 1000);

        if (diffSeconds < 60) return '방금 전';
        const diffMinutes = Math.floor(diffSeconds / 60);
        if (diffMinutes < 60) return `${diffMinutes}분 전`;
        const diffHours = Math.floor(diffMinutes / 60);
        if (diffHours < 24) return `${diffHours}시간 전`;

        const diffDays = Math.floor(diffHours / 24);
        if (diffDays === 1) return '어제';
        if (diffDays < 7) return `${diffDays}일 전`;

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;

      } catch (e) {
        console.error("Error formatting date:", dateString, e);
        return '날짜 형식 오류';
      }
    },

    // 댓글 내용 포맷 (HTML 이스케이프 및 개행 처리)
    formatCommentContent(content) { // 'content' 파라미터 사용됨
      if (!content) return '';
      return content.replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/\n/g, '<br>');
    },

    // formatPostDate는 formatDate를 호출하는 래퍼였으므로, formatDate 직접 사용 권장
    // 만약 템플릿에서 formatPostDate(post.createdAt) 형태로 사용했다면 formatDate(post.createdAt)으로 변경하거나
    // formatPostDate를 유지하고 싶다면 아래처럼 구현
    formatPostDate(date) { // 'date' 파라미터 사용됨
      return this.formatDate(date);
    },

    // 파일 크기 포맷 (KB, MB 등으로 변환)
    formatFileSize(bytes) {
      if (bytes === 0) return '0 Bytes';
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
      // bytes가 숫자가 아니거나 음수일 경우 처리
      if (isNaN(bytes) || bytes < 0) return 'N/A';

      const i = Math.floor(Math.log(bytes) / Math.log(k));

      // i가 sizes 배열 범위를 벗어나는 경우 처리 (매우 큰 파일)
      if (i >= sizes.length) return parseFloat((bytes / Math.pow(k, sizes.length - 1)).toFixed(1)) + ' ' + sizes[sizes.length - 1];

      return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    },

    // 댓글 수정 함수 (현재 미구현 상태 알림)
    editComment(comment, parentId = null) {
      // comment 객체와 부모 ID(답글일 경우)를 받을 수 있음
      alert('댓글 수정 기능은 현재 준비 중입니다.');
      console.log("수정 시도:", comment, "부모 ID:", parentId);
      // 실제 구현 시:
      // 1. 수정 UI 표시 (예: 기존 댓글 영역을 입력 폼으로 변경)
      // 2. 수정 내용 입력 받기
      // 3. 수정 API 호출 (authService.updateComment 등 필요)
      // 4. 성공 시 UI 업데이트
    },

  },
};
</script>

<style>
/* 모던한 게시글 상세 페이지 스타일 */
.post-detail {
  background-color: #ffffff;
  min-height: 100vh;
}

.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 2.5rem;
}

/* 로딩 상태 스타일 */
.loading-state, .not-found {
  text-align: center;
  padding: 4rem 0;
  color: #555;
}

.loading-state .spinner {
  border: 3px solid rgba(0, 0, 0, 0.08);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border-left-color: #444;
  animation: spin 1s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 게시글 컨테이너 스타일 */
.post-container {
  background-color: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 3rem;
  margin-bottom: 2rem;
}

/* 게시글 헤더 스타일 */
.post-header {
  margin-bottom: 2.5rem;
  border-bottom: 1px solid #eaeaea;
  padding-bottom: 1.5rem;
}

.post-title {
  font-size: 2.2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 1rem;
  line-height: 1.3;
  letter-spacing: -0.5px;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #666;
  font-size: 0.9rem;
}

.menu-tag {
  background-color: #f2f2f2;
  color: #444;
  padding: 0.3rem 0.7rem;
  border-radius: 2px;
  font-size: 0.85rem;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.separator {
  color: #ccc;
}

/* 게시글 내용 스타일 */
.post-content {
  margin-bottom: 2.5rem;
}

.post-body.ql-editor {
  padding: 0;
  font-size: 1.05rem;
  line-height: 1.8;
  color: #333;
  word-break: break-word;
}

.post-body img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1.5rem 0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}

.post-body blockquote {
  border-left: 3px solid #e0e0e0;
  margin: 1.5rem 0;
  padding: 0.5rem 0 0.5rem 1.5rem;
  color: #555;
  font-style: italic;
}

.post-body .ql-syntax {
  background-color: #f8f8f8;
  color: #333;
  padding: 1.2rem;
  margin: 1.5rem 0;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9rem;
  border: 1px solid #e5e5e5;
  overflow-x: auto;
}

.post-body table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  font-size: 0.95rem;
}

.post-body th, .post-body td {
  border: 1px solid #e0e0e0;
  padding: 0.8rem 1rem;
  text-align: left;
}

.post-body th {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* 게시글 액션 버튼 스타일 */
.post-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eaeaea;
}

.edit-button, .back-button, .delete-button {
  padding: 0.6rem 1.2rem;
  border-radius: 3px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  cursor: pointer;
  font-size: 0.9rem;
  letter-spacing: 0.3px;
}

.edit-button {
  background-color: #555;
  color: white;
}

.edit-button:hover {
  background-color: #444;
}

.back-button {
  background-color: #f2f2f2;
  color: #333;
  border-color: #e0e0e0;
}

.back-button:hover {
  background-color: #e8e8e8;
}

.delete-button {
  background-color: #f44336;
  color: white;
}

.delete-button:hover {
  background-color: #e53935;
}

/* 댓글 섹션 스타일 */
.comments-section {
  background-color: #ffffff;
  padding: 2.5rem;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.comments-header h3 {
  margin-bottom: 1.5rem;
  font-size: 1.3rem;
  color: #333;
  font-weight: 600;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #eaeaea;
}

.no-comments {
  color: #777;
  text-align: center;
  padding: 2rem 0;
  font-size: 0.95rem;
}

/* 댓글 스타일 */
.comment {
  border-bottom: 1px solid #eaeaea;
  padding: 1.5rem 0;
}

.comment:last-child {
  border-bottom: none;
}

.comment p, .reply p {
  margin-bottom: 0.75rem;
  line-height: 1.6;
  color: #444;
}

.comment-meta, .reply-meta {
  color: #777;
  font-size: 0.85rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.comment-meta i, .reply-meta i {
  color: #999;
  margin-right: 0.25rem;
}

.reply-button {
  margin-left: auto;
  background-color: transparent;
  border: 1px solid #d0d0d0;
  border-radius: 2px;
  padding: 0.3rem 0.7rem;
  font-size: 0.8rem;
  color: #555;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reply-button:hover {
  background-color: #f5f5f5;
  border-color: #c0c0c0;
}

.comment-actions {
  margin-left: auto;
  display: flex;
  gap: 0.75rem;
}

.action-button {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 0;
  transition: color 0.2s ease;
}

.action-button:hover {
  color: #333;
}

.edit-comment-btn {
  color: #555;
}

.edit-comment-btn:hover {
  color: #333;
}

.delete-comment-btn {
  color: #777;
}

.delete-comment-btn:hover {
  color: #f44336;
}

/* 답글 폼 스타일 */
.reply-form {
  margin-top: 1.25rem;
  background-color: #f9f9f9;
  padding: 1.25rem;
  border-radius: 3px;
  border: 1px solid #e5e5e5;
}

.reply-input {
  width: 100%;
  padding: 0.8rem 1rem;
  border: 1px solid #d5d5d5;
  border-radius: 3px;
  resize: vertical;
  margin-bottom: 1rem;
  min-height: 80px;
  font-size: 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.reply-input:focus {
  outline: none;
  border-color: #aaa;
  box-shadow: 0 0 0 2px rgba(170, 170, 170, 0.15);
}

.reply-form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.submit-reply-button, .cancel-reply-button {
  padding: 0.6rem 1.2rem;
  border-radius: 3px;
  font-size: 0.9rem;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.submit-reply-button {
  background-color: #555;
  color: white;
}

.submit-reply-button:hover {
  background-color: #444;
}

.submit-reply-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-reply-button {
  background-color: #e0e0e0;
  color: #333;
}

.cancel-reply-button:hover {
  background-color: #d0d0d0;
}

/* 답글 스타일 */
.replies {
  margin-top: 1.25rem;
  border-left: 2px solid #e0e0e0;
  padding-left: 1.5rem;
  margin-left: 1rem;
}

.reply {
  background-color: #f9f9f9;
  border-radius: 3px;
  padding: 1.25rem;
  margin-bottom: 1rem;
  border: 1px solid #e5e5e5;
}

/* 댓글 폼 스타일 */
.comment-form {
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #eaeaea;
}

.comment-input {
  width: 100%;
  min-height: 120px;
  padding: 1rem;
  border: 1px solid #d5d5d5;
  border-radius: 3px;
  resize: vertical;
  margin-bottom: 1.25rem;
  font-size: 0.95rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.comment-input:focus {
  outline: none;
  border-color: #aaa;
  box-shadow: 0 0 0 2px rgba(170, 170, 170, 0.15);
}

.comment-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.submit-comment-button {
  background-color: #555;
  color: white;
  border: none;
  padding: 0.7rem 1.5rem;
  border-radius: 3px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
  letter-spacing: 0.3px;
}

.submit-comment-button:hover {
  background-color: #444;
}

.submit-comment-button:disabled {
  background-color: #999;
  cursor: not-allowed;
}

.login-prompt {
  font-size: 0.9rem;
  color: #777;
  margin-top: 0.75rem;
}

.login-prompt a {
  color: #555;
  font-weight: 500;
  text-decoration: none;
}

.login-prompt a:hover {
  text-decoration: underline;
}

/* 반응형 스타일 */
@media (max-width: 768px) {
  .container {
    padding: 1.5rem;
  }

  .post-container, .comments-section {
    padding: 1.5rem;
  }

  .post-title {
    font-size: 1.8rem;
  }
}
</style>
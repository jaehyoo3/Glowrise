<template>
  <div class="post-create">
    <NavBar/>
    <div class="container">
      <div class="post-create-content">
        <div class="post-create-header">
          <h1>새 게시글 작성</h1>
        </div>

        <div v-if="isLoading" class="loading-state">
          <span>로딩 중...</span>
        </div>

        <div v-else class="post-form-container">
          <form @submit.prevent="createPost" class="post-form">
            <div class="form-group">
              <label for="menuSelect" class="form-label">메뉴 선택 (필수)</label>
              <select
                  v-model="newPost.menuId"
                  class="form-control"
                  id="menuSelect"
                  required
              >
                <option value="" disabled>메뉴를 선택하세요</option>
                <option
                    v-for="menu in allMenus"
                    :key="menu.id"
                    :value="menu.id"
                    :disabled="isParentMenu(menu.id)"
                >
                  {{ menu.name }} {{ menu.parentId ? `(하위: ${getParentName(menu.parentId)})` : '' }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label for="postTitle" class="form-label">제목</label>
              <input
                  v-model="newPost.title"
                  class="form-control"
                  id="postTitle"
                  required
                  placeholder="게시글 제목을 입력하세요"
              />
            </div>

            <div class="form-group">
              <label for="postContent" class="form-label">내용</label>
              <textarea
                  v-model="newPost.content"
                  class="form-control"
                  id="postContent"
                  required
                  placeholder="게시글 내용을 작성하세요"
                  rows="10"
              ></textarea>
            </div>

            <div class="form-group">
              <label for="postFiles" class="form-label">첨부 파일</label>
              <input
                  type="file"
                  multiple
                  class="form-control"
                  id="postFiles"
                  @change="handleFileChange"
              />
            </div>

            <div class="form-actions">
              <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="isPosting"
              >
                {{ isPosting ? '작성 중...' : '작성하기' }}
              </button>
              <router-link
                  :to="`/blog/${blogUrl}`"
                  class="btn btn-secondary"
              >
                취소
              </router-link>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import NavBar from '@/components/NavBar.vue';
import authService from '@/services/authService';

export default {
  name: 'PostCreate',
  components: {NavBar},
  props: {
    blogUrl: String, // URL에서 블로그 URL 가져오기
  },
  data() {
    return {
      blog: null,
      menus: [],
      allMenus: [], // 계층 구조를 평평하게 펼친 메뉴 목록
      isLoading: true,
      isPosting: false,
      newPost: {
        title: '',
        content: '',
        menuId: '',
        userId: null,
      },
      postFiles: [],
    };
  },
  async created() {
    await this.loadBlogAndMenus();
  },
  methods: {
    async loadBlogAndMenus() {
      try {
        this.isLoading = true;
        const blogUrl = this.blogUrl || this.$route.params.blogUrl;
        console.log('Fetching blog with URL:', blogUrl);

        this.blog = await authService.getBlogByUrl(blogUrl);
        console.log('Loaded blog:', JSON.stringify(this.blog));

        if (this.blog) {
          this.menus = await authService.getMenusByBlogId(this.blog.id);
          console.log('Loaded menus:', JSON.stringify(this.menus));
          this.allMenus = this.flattenMenus(this.menus);
          console.log('Flattened allMenus:', JSON.stringify(this.allMenus));

          const user = await authService.getCurrentUser();
          console.log('Current user:', JSON.stringify(user));
          if (user && user.id) {
            this.newPost.userId = user.id;
            if (this.blog.userId && this.blog.userId !== user.id) {
              throw new Error('이 블로그에 게시글을 작성할 권한이 없습니다.');
            }
          } else {
            throw new Error('사용자 정보가 없습니다.');
          }

          const queryMenuId = this.menuId || this.$route.query.menuId;
          if (queryMenuId && this.allMenus.some(menu => menu.id === Number(queryMenuId))) {
            this.newPost.menuId = Number(queryMenuId);
            console.log('Pre-selected menuId from query:', this.newPost.menuId);
          }

          // 부모 메뉴 확인 로그
          this.allMenus.forEach(menu => {
            console.log(`Menu ${menu.id} (${menu.name}) is parent: ${this.isParentMenu(menu.id)}`);
          });
        }
      } catch (error) {
        console.error('블로그 또는 메뉴 로드 실패:', error);
        this.$router.push('/login');
      } finally {
        this.isLoading = false;
      }
    },
    flattenMenus(menus) {
      const result = [];
      const flatten = (menuList) => {
        menuList.forEach(menu => {
          result.push(menu);
          if (menu.subMenus && menu.subMenus.length > 0) {
            flatten(menu.subMenus);
          }
        });
      };
      flatten(menus);
      return result;
    },
    getParentName(parentId) {
      const parent = this.allMenus.find(menu => menu.id === parentId);
      return parent ? parent.name : '';
    },
    isParentMenu(menuId) {
      return this.allMenus.some(menu => menu.parentId === menuId);
    },
    handleFileChange(event) {
      this.postFiles = Array.from(event.target.files);
      console.log('Selected files:', this.postFiles);
    },
    async createPost() {
      if (!this.newPost.menuId) {
        alert('메뉴를 선택해야 게시글을 작성할 수 있습니다.');
        return;
      }
      this.isPosting = true;
      try {
        const postData = {...this.newPost};
        const createdPost = await authService.createPost(postData, this.postFiles);
        console.log('Created post:', JSON.stringify(createdPost));
        this.$router.push(`/blog/${this.blog.url}/${this.newPost.menuId}`);
      } catch (error) {
        console.error('게시글 작성 실패:', error);
        alert('게시글 작성 실패: ' + (error.response?.data?.message || error.message));
      } finally {
        this.isPosting = false;
      }
    },
  },
};
</script>
<style scoped>
.post-create {
  background-color: #f8f9fa;
  min-height: 100vh;
  color: #333;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
}

.post-create-content {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 2rem;
}

.post-create-header {
  margin-bottom: 2rem;
  text-align: center;
}

.post-create-header h1 {
  font-size: 2.5rem;
  font-weight: 700;
  color: #000;
  border-bottom: 2px solid #000;
  padding-bottom: 0.5rem;
}

.loading-state {
  text-align: center;
  padding: 2rem;
  font-size: 1.2rem;
}

.post-form-container {
  max-width: 600px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.form-control {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  background-color: #f8f9fa;
  transition: border-color 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #000;
}

.form-control::placeholder {
  color: #888;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 2rem;
}

.btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  text-align: center;
  font-size: 1rem;
}

.btn-primary {
  background-color: #000;
  color: white;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #f8f9fa;
  color: #000;
  border: 1px solid #000;
}

@media (max-width: 768px) {
  .container {
    padding: 1rem;
  }

  .post-create-content {
    padding: 1.5rem;
  }

  .post-create-header h1 {
    font-size: 2rem;
  }

  .form-actions {
    flex-direction: column;
    gap: 0.5rem;
  }

  .btn {
    width: 100%;
  }
}
</style>
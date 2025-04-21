<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-section">
          <h3>Glowrise</h3>
          <p>당신의 이야기를 세상과 나누세요.</p>
        </div>

        <div class="footer-section">
          <h3>이용안내</h3>
          <ul>
            <li><a href="/terms">이용약관</a></li>
            <li><a href="/privacy">개인정보처리방침</a></li>
            <li><a href="/faq">자주 묻는 질문</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h3>고객지원</h3>
          <ul>
            <li><a href="/support">고객센터</a></li>
            <li><a href="/contact">문의하기</a></li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <p>© 2025 Glowrise. All rights reserved.</p>
        <div class="social-links">
          <a aria-label="Facebook" href="#"><i class="fa-brands fa-facebook"></i></a>
          <a aria-label="Twitter" href="#"><i class="fa-brands fa-x-twitter"></i></a>
          <a aria-label="Instagram" href="#"><i class="fa-brands fa-instagram"></i></a>
        </div>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'FooterComponent'
}
</script>

<style scoped>
.footer {
  background-color: #f9f9f9;
  border-top: 1px solid #eaeaea;
  padding: 2.5rem 0 1.5rem;
  color: #666;
  font-family: 'Noto Sans KR', 'Malgun Gothic', sans-serif;
  margin-top: 3rem;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 2rem;
  margin-bottom: 2rem;
}

.footer-section h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #444;
  margin-bottom: 1rem;
}

.footer-section p {
  font-size: 0.9rem;
  line-height: 1.5;
  margin-bottom: 1rem;
}

.footer-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-section ul li {
  margin-bottom: 0.5rem;
}

.footer-section ul li a {
  color: #666;
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.2s ease;
}

.footer-section ul li a:hover {
  color: #333;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 1.5rem;
  border-top: 1px solid #eee;
  font-size: 0.85rem;
}

.footer-bottom p {
  margin: 0;
  color: #888;
}

.social-links {
  display: flex;
  gap: 1rem;
}

.social-links a {
  color: #777;
  font-size: 1.2rem;
  transition: color 0.2s ease;
}

.social-links a:hover {
  color: #444;
}

@media (max-width: 768px) {
  .footer {
    padding: 2rem 0 1rem;
  }

  .footer-container {
    padding: 0 1.5rem;
  }

  .footer-content {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 1rem;
    text-align: center;
  }
}
</style>
# Glowrise (Blog Service - Toy Project)

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-blue.svg)](https://spring.io/projects/spring-security)
[![JPA / Hibernate](https://img.shields.io/badge/JPA_/_Hibernate-Data-purple.svg)](https://spring.io/projects/spring-data-jpa)
[![QueryDSL](https://img.shields.io/badge/QueryDSL-5.x-darkgreen.svg)](http://querydsl.com/)
[![JWT](https://img.shields.io/badge/Auth-JWT-yellowgreen.svg)](https://jwt.io/)
[![OAuth2](https://img.shields.io/badge/Auth-OAuth2-green.svg)](https://oauth.net/2/)
[![Kafka](https://img.shields.io/badge/Messaging-Kafka-black.svg)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Cache-Redis-red.svg)](https://redis.io/)
[![WebSocket](https://img.shields.io/badge/Realtime-WebSocket-blueviolet.svg)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
[![Docker](https://img.shields.io/badge/Docker-Container-blue.svg)](https://www.docker.com/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-teal.svg)](https://gradle.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
<br>**Glowrise**
는 개인 포트폴리오 목적으로 개발된 풀스택 토이 프로젝트입니다. **본인이 직접 개발한 Spring Boot 기반 백엔드 서버**와 **AI 도구를 활용하여 생성된 Vue.js 프론트엔드**로 구성되어
있으며, [블로그 서비스] 애플리케이션의 기반을 제공합니다.

**특히 백엔드 개발에 중점을 두어**, RESTful API 설계, JWT & OAuth2를 이용한 보안 구현, JPA & QueryDSL 데이터 처리, WebSocket/Kafka 기반 실시간 기능, Docker
컨테이너화 등 현대적인 백엔드 개발 기술을 적용하고 시연하는 것을 목표로 했습니다.

## ✨ 프로젝트 목표 및 동기

* **Spring Boot 생태계 숙달:** Spring Boot, Spring Security, Spring Data JPA 등 핵심 프레임워크를 활용하여 실제 동작하는 웹 애플리케이션 백엔드를 구축하고 이해도를
  높이는 것을 목표로 했습니다.
* **최신 백엔드 기술 적용:** JWT 인증, OAuth2 소셜 로그인, QueryDSL을 이용한 동적 쿼리, Kafka와 WebSocket을 사용한 실시간 알림 등 현대적인 웹 서비스에 필요한 기술들을 직접
  구현하고 경험했습니다.
* **포트폴리오 제작:** 백엔드 개발자로서 필요한 기술 스택과 문제 해결 능력을 보여줄 수 있는 실질적인 프로젝트 결과물을 만드는 것을 목표로 삼았습니다.
* **유지보수 및 확장성:** DTO 패턴 (MapStruct 활용), 계층형 아키텍처(Layered Architecture), JPA Auditing 등을 적용하여 코드의 유지보수성과 확장성을 고려하며
  개발했습니다.
* **AI 활용 탐구:** **프론트엔드 부분은 AI 도구를 사용하여 개발함으로써, AI 기반 개발 워크플로우를 실험하고 백엔드 API를 위한 인터페이스를 빠르게 구축하는 경험을 하였습니다.**

## 🚀 주요 기능 (백엔드 중심)

* **👤 사용자 관리:** 회원가입, 로그인, 사용자 정보 조회/수정, 역할(Role) 기반 접근 제어 (Spring Security).
* **🔒 인증 (Authentication):**
    * **JWT 기반 인증:** Access Token, Refresh Token을 사용한 stateless 인증 방식 구현.
    * **OAuth2 소셜 로그인:** Google, Naver 계정을 이용한 간편 로그인 기능 구현.
* **🔐 인가 (Authorization):** Spring Security를 활용하여 API 엔드포인트별 접근 권한 관리.
* **📝 블로그/게시글 관리:** 게시글 CRUD (생성, 조회, 수정, 삭제), QueryDSL을 활용한 동적 검색 및 페이징 처리.
* **💬 댓글 관리:** 게시글에 대한 댓글 CRUD 기능.
* **📄 파일 업로드/관리:** 파일 업로드 및 관련 정보 관리 기능 (파일 저장 방식은 [로컬, DB 저장] 입니다).
* **🧭 메뉴 관리:** 동적인 사이트 메뉴 구성/관리 기능
* **🔔 실시간 알림:** WebSocket을 통해 사용자에게 실시간 알림 전송 (예: 새 댓글, 중요 공지). Kafka를 메시지 큐로 사용하여 알림 발송 로직을 비동기 처리 및 분리.

## 🛠️ 기술 스택

### **Backend (직접 개발)**

* **Language:** `Java 17`
* **Framework:** `Spring Boot 3.x`
* **Data Access:** `Spring Data JPA`, `QueryDSL 5.x`, `Hibernate`
* **Database:** `MySQL` (Docker), `H2` (Test)
* **Security:** `Spring Security 6.x`, `JWT`, `OAuth2 Client` (Google, Naver)
* **Real-time & Messaging:** `Spring WebSocket`, `Kafka`
* **Cache/Session Store:** `Redis` (Docker)
* **API:** `RESTful API`
* **DTO Mapping:** `MapStruct`
* **Build Tool:** `Gradle`
* **Containerization:** `Docker`, `Docker Compose`
* **Testing:** `JUnit 5`, `Spring Boot Test`, `Mockito` (추정)
* **Utilities:** `Lombok`

### **Frontend (AI 생성)**

* **Framework:** `Vue.js`
* **Note:** **프론트엔드 UI 및 관련 로직은 [Clude, Grok, Gemini]를 활용하여 생성헀습니다. 이 부분은 AI 개발 지원 도구의 가능성을 탐색하기 위한 실험적인 요소이며, 본 포트폴리오의
  핵심 기술 시연은 백엔드에 집중되어 있습니다.**

## 🏗️ 아키텍처 (백엔드)

* **Layered Architecture:** Controller - Service - Repository 구조를 따라 역할 분리.
* **RESTful API:** 표준 HTTP 메서드와 상태 코드를 준수하여 API 설계.
* **DTO Pattern:** 데이터 전송 객체(DTO)를 사용하여 계층 간 데이터 전달 및 View 종속성 분리 (MapStruct으로 매핑 자동화).
* **JPA Auditing:** `AbstractAuditingEntity`와 `AuditorAwareImpl`을 통해 생성/수정 시간 및 사용자 자동 기록.
* **Asynchronous Processing:** Kafka를 이용한 알림 메시지 발행/구독 모델로 비동기 처리 구현.

## 🔑 API 주요 엔드포인트

Authentication & User
POST /api/users/signup # 회원가입
POST /login # JWT 로그인
GET /oauth2/authorization/{provider} # 소셜 로그인 시작 (provider: google, naver)
GET /api/users/me # 내 정보 조회

Posts & Comments
GET /api/posts # 게시글 목록 조회 (페이징, 검색 가능)
POST /api/posts # 게시글 생성
GET /api/posts/{postId} # 게시글 상세 조회
PUT /api/posts/{postId} # 게시글 수정
DELETE /api/posts/{postId} # 게시글 삭제
POST /api/posts/{postId}/comments # 댓글 생성
GET /api/posts/{postId}/comments # 특정 게시글 댓글 목록 조회

Notifications
GET /api/notifications # (예시) 내 알림 목록 조회

WebSocket Endpoint: /ws (설정 확인 필요)
Files
POST /api/files/upload # 파일 업로드

## 🤔 도전 과제 및 학습 내용 (Tricky Aspects & What I Learned) - 백엔드 중심

* **JWT 인증 흐름 설계:** Stateless 환경에서 Access/Refresh Token을 안전하게 관리하고 재발급하는 로직 구현. Spring Security Filter 체인 커스터마이징 (
  `JWTFilter`).
* **OAuth2 통합:** 여러 소셜 로그인 제공자(Google, Naver)의 각기 다른 응답 형식을 처리하고, 기존 사용자 시스템과 연동 (`CustomOAuth2UserService`, DTO 설계).
  로그인 성공 후 JWT 발급 로직 연동 (`CustomSuccessHandler`).
* **WebSocket & Kafka 연동:** 실시간 통신을 위한 WebSocket 설정 및 Kafka를 이용한 안정적인 비동기 알림 시스템 구축. Producer/Consumer 구현 및 메시지 포맷 정의 (
  `NotificationEvent`, `NotificationDTO`).
* **QueryDSL 활용:** 복잡한 검색 조건과 동적 정렬, 효율적인 페이징 처리를 위한 QueryDSL 설정 및 Repository 구현 (`QueryDslPagingUtil`, Custom
  Repository).
* **Docker Compose 환경 구축:** 다중 컨테이너(애플리케이션, DB, Redis, Kafka) 환경을 `docker-compose.yml` 파일로 정의하고 관리. 각 서비스 간 네트워크 설정 및
  의존성 관리.
* **MapStruct 적용:** 반복적인 DTO-Entity 변환 코드를 어노테이션 기반으로 자동 생성하여 생산성 향상 및 코드 간결화.

## ⚙️ 설치 및 실행 방법

**Prerequisites:**

* `Java 17` or higher
* `Docker` & `Docker Compose`
* `Vue` npm run serve

**Steps:**

1. **저장소 복제:**
   ```bash
   git clone [이 저장소의 URL]
   cd Glowrise
   ```

2. **(선택) application.yaml 설정:**
    * `src/main/resources/application.yaml` 파일에서 DB 접속 정보, JWT 시크릿 키, OAuth2 클라이언트 ID/Secret 등을 필요에 따라 수정합니다. (특히 OAuth2
      정보는 직접 발급받아 입력해야 합니다.)

3. **Docker Compose 실행 (백엔드 의존성):** (DB, Redis, Kafka 컨테이너 실행)
   ```bash
   docker-compose up -d
   ```
    * 컨테이너들이 정상적으로 실행될 때까지 잠시 기다립니다.

4. **백엔드 애플리케이션 빌드 및 실행 (Gradle Wrapper 사용):**
   ```bash
   ./gradlew bootRun
   ```
    * 또는 빌드 후 jar 실행:
        ```bash
        ./gradlew build -x test # 테스트 제외하고 빌드
        java -jar build/libs/Glowrise-0.0.1-SNAPSHOT.jar # 생성된 jar 파일 실행
        ```

5. **백엔드 서버 접속:**
    * 서버가 시작되면 `http://localhost:8080` (또는 `application.yaml`에 설정된 포트) 로 API 요청을 보낼 수 있습니다.

6. **(선택) 프론트엔드 실행 (AI 생성 코드):**
    * **[프론트엔드 코드 위치 안내, 예: `/frontend` 폴더로 이동]**
    * **[프론트엔드 의존성 설치 명령어, 예: `npm install` 또는 `yarn install`]**
    * **[프론트엔드 개발 서버 실행 명령어, 예: `npm run serve` 또는 `yarn serve`]**
    * **[프론트엔드 접속 URL 안내, 예: `http://localhost:8081`]**

## 📈 향후 개선 사항 (Future Improvements) - 백엔드 중심

* **테스트 커버리지 확대:** 단위 테스트, 통합 테스트, E2E 테스트 추가하여 코드 안정성 확보.
* **클라우드 스토리지 연동:** 파일 저장을 로컬 대신 AWS S3 등 클라우드 스토리지 사용하도록 개선.
* **모니터링 및 로깅 강화:** Actuator, Prometheus, Grafana 등을 이용한 모니터링 환경 구축 및 ELK Stack 등을 활용한 로그 중앙 관리 시스템 도입 고려.
* **API 문서 자동화:** Swagger/OpenAPI 연동하여 API 명세 자동 생성 및 UI 제공.
* **세분화된 예외 처리:** 사용자 친화적인 오류 메시지 제공 및 상황별 예외 처리 로직 개선.
* **인덱싱 및 쿼리 최적화:** 대용량 데이터를 가정한 DB 인덱싱 전략 수립 및 느린 쿼리(Slow Query) 분석 및 개선.
* **(장기적) 프론트엔드 직접 개발:** AI 생성 코드를 기반으로 직접 프론트엔드 개발을 진행하여 Full-Stack 역량 강화.

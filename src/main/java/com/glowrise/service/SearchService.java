package com.glowrise.service;

import com.glowrise.domain.Post;
import com.glowrise.domain.QPost;
import com.glowrise.domain.QUser;
import com.glowrise.domain.User;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.dto.SearchResultDTO;
import com.glowrise.service.dto.UserDTO;
import com.glowrise.service.mapper.PostMapper;
import com.glowrise.service.mapper.UserMapper;
import com.glowrise.service.util.QueryDslPagingUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int SNIPPET_MAX_LENGTH = 150;
    private final JPAQueryFactory queryFactory;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final QueryDslPagingUtil queryDslPagingUtil;

    public SearchResultDTO searchIntegrated(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) {
            return new SearchResultDTO(Page.empty(pageable), Page.empty(pageable));
        }

        QPost post = QPost.post;
        QUser user = QUser.user;

        // --- 게시글 검색 ---
        BooleanBuilder postPredicate = buildPostPredicate(query, post, user); // FTS 적용된 Predicate 사용
        JPAQuery<Post> postBaseQuery = queryFactory
                .selectFrom(post)
                .leftJoin(post.author, user).fetchJoin()
                .where(postPredicate)
                .orderBy(post.createdDate.desc()); // 필요시 Pageable의 sort 정보 활용 로직 추가

        Page<Post> postEntityPage = queryDslPagingUtil.getPage(postBaseQuery, pageable);
        Page<PostDTO> postDtoPage = postEntityPage.map(this::mapPostToDtoWithSnippet);

        // --- 사용자 검색 ---
        BooleanBuilder userPredicate = buildUserPredicate(query, user);
        JPAQuery<User> userBaseQuery = queryFactory
                .selectFrom(user)
                .where(userPredicate)
                .orderBy(user.nickName.asc());

        Page<User> userEntityPage = queryDslPagingUtil.getPage(userBaseQuery, pageable);
        Page<UserDTO> userDtoPage = userEntityPage.map(userMapper::toDto);

        return new SearchResultDTO(postDtoPage, userDtoPage);
    }

    /**
     * 게시글 검색 조건 생성 (MySQL FTS 적용)
     */
    private BooleanBuilder buildPostPredicate(String query, QPost post, QUser user) {
        BooleanBuilder predicate = new BooleanBuilder();

        // 제목 검색 (기존 방식 유지)
        predicate.or(post.title.containsIgnoreCase(query));

        // 내용 검색 (MySQL Full-Text Search 사용)
        // predicate.or(post.content.containsIgnoreCase(query)); // 기존 방식 주석 처리 또는 삭제
        predicate.or(mysqlMatchAgainst(post.content, query)); // FTS 함수 호출로 변경

        // 작성자 닉네임 검색
        predicate.or(post.author.nickName.containsIgnoreCase(query));

        // 작성자 이메일 검색
        predicate.or(post.author.email.containsIgnoreCase(query));

        return predicate;
    }

    /**
     * 사용자 검색 조건 생성
     */
    private BooleanBuilder buildUserPredicate(String query, QUser user) {
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.or(user.nickName.containsIgnoreCase(query));
        predicate.or(user.email.containsIgnoreCase(query));
        return predicate;
    }

    /**
     * Post 엔티티 -> PostDTO 변환 (Jsoup 스니펫 생성 포함)
     */
    private PostDTO mapPostToDtoWithSnippet(Post post) {
        PostDTO dto = postMapper.toDto(post);
        String originalContent = post.getContent();
        if (StringUtils.hasText(originalContent)) {
            String plainText = Jsoup.parse(originalContent).text();
            String snippet = plainText.length() > SNIPPET_MAX_LENGTH
                    ? plainText.substring(0, SNIPPET_MAX_LENGTH) + "..."
                    : plainText;
            dto.setContentSnippet(snippet);
        } else {
            dto.setContentSnippet("");
        }
        return dto;
    }

    /**
     * MySQL의 MATCH AGAINST 구문을 QueryDSL에서 사용하기 위한 Helper 메소드.
     * (BOOLEAN MODE 사용 예시)
     *
     * @param columnPath 검색 대상 컬럼 (예: QPost.post.content)
     * @param searchTerm 검색어
     * @return QueryDSL BooleanExpression
     */
    private BooleanExpression mysqlMatchAgainst(StringPath columnPath, String searchTerm) {
        // BOOLEAN MODE는 검색어 구문에 따라 동작이 달라짐 (+, -, *, "" 등 활용 가능)
        // 여기서는 기본적인 단어 포함 검색을 위해 검색어를 그대로 전달하는 예시
        // 필요시 searchTerm 가공 필요 (예: '+' 추가하여 필수 단어 지정 등)
        // 예: return Expressions.booleanTemplate("MATCH({0}) AGAINST({1} IN BOOLEAN MODE)", columnPath, "+" + searchTerm + "*");
        return Expressions.booleanTemplate("MATCH({0}) AGAINST({1} IN BOOLEAN MODE)",
                columnPath,
                searchTerm); // 검색어를 그대로 전달
    }
}
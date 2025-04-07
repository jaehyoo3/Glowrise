package com.glowrise.service.util;

import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryDslPagingUtil {
    /**
     * QueryDSL을 사용한 공통 페이징 처리
     *
     * @param baseQuery 기본 쿼리
     * @param pageable  페이징 정보
     * @param <T>       반환 DTO 타입
     * @return 페이징된 DTO 목록
     */
    public <T> Page<T> getPage(JPQLQuery<T> baseQuery, Pageable pageable) {
        // 페이징 적용 및 결과 조회
        List<T> results = baseQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        long total = baseQuery.fetchCount();

        return new PageImpl<>(results, pageable, total);
    }
}
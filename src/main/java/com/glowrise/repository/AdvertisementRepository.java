package com.glowrise.repository;

import com.glowrise.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // 현재 시간 기준 활성화되어 있고, 표출 기간 내에 있는 광고 목록 조회 (표출 순서, 생성일 순 정렬)
    @Query("SELECT a FROM Advertisement a WHERE a.isActive = true AND a.startDate <= :now AND a.endDate >= :now ORDER BY a.displayOrder ASC, a.createdDate DESC")
    List<Advertisement> findActiveAdvertisements(@Param("now") LocalDateTime now);
}
package com.glowrise.repository;

import com.glowrise.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {


    List<Advertisement> findByActiveTrueOrderByDisplayOrderAsc();
}
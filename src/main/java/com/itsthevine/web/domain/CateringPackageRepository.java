package com.itsthevine.web.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CateringPackageRepository extends JpaRepository<CateringPackage, Long> {

    List<CateringPackage> findAllByOrderByPositionAsc();
}

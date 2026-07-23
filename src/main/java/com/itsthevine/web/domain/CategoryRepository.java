package com.itsthevine.web.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByPositionAsc();

    Optional<Category> findByNameIgnoreCase(String name);
}

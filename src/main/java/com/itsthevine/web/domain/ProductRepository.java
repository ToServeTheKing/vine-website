package com.itsthevine.web.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByPositionAsc();

    List<Product> findAllByCategoryOrderByPositionAsc(String category);
}

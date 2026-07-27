package com.itsthevine.web.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CateringNoteRepository extends JpaRepository<CateringNote, Long> {

    List<CateringNote> findAllByOrderByPositionAsc();
}

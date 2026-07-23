package com.itsthevine.web.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactEnquiryRepository extends JpaRepository<ContactEnquiry, Long> {

    /** Newest first — the admin screen reads like an inbox. */
    List<ContactEnquiry> findAllByOrderByCreatedAtDesc();
}

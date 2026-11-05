package com.companya.resort.repository;

import com.companya.resort.entity.DraftStatus;
import com.companya.resort.entity.EnquiryDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryDraftRepository extends JpaRepository<EnquiryDraft, Long> {

    List<EnquiryDraft> findByEnquiryIdOrderByCreatedAtDesc(Long enquiryId);

    Optional<EnquiryDraft> findFirstByEnquiryIdOrderByCreatedAtDesc(Long enquiryId);

    Optional<EnquiryDraft> findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(Long enquiryId, DraftStatus status);
}

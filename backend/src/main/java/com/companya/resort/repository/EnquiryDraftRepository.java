package com.companya.resort.repository;

import com.companya.resort.entity.DraftStatus;
import com.companya.resort.entity.EnquiryDraft;
import com.companya.resort.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryDraftRepository extends JpaRepository<EnquiryDraft, Long> {

    List<EnquiryDraft> findByEnquiryIdOrderByCreatedAtDesc(Long enquiryId);

    Optional<EnquiryDraft> findFirstByEnquiryIdOrderByCreatedAtDesc(Long enquiryId);

    Optional<EnquiryDraft> findFirstByEnquiryIdAndStatusOrderByCreatedAtDesc(Long enquiryId, DraftStatus status);

    @Modifying
    @Query("UPDATE EnquiryDraft d SET d.status = :newStatus WHERE d.id = :draftId AND d.status = :expectedStatus")
    int atomicClaimDraft(
            @Param("draftId") Long draftId,
            @Param("expectedStatus") DraftStatus expectedStatus,
            @Param("newStatus") DraftStatus newStatus
    );

    @Modifying
    @Query("UPDATE EnquiryDraft d SET d.status = :newStatus WHERE d.id = :draftId AND d.status = :expectedStatus")
    int atomicDiscardDraft(
            @Param("draftId") Long draftId,
            @Param("expectedStatus") DraftStatus expectedStatus,
            @Param("newStatus") DraftStatus newStatus
    );
}

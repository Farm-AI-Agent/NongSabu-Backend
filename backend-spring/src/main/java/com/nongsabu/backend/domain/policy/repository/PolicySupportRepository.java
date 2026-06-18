package com.nongsabu.backend.domain.policy.repository;

import com.nongsabu.backend.domain.policy.entity.PolicySupport;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicySupportRepository extends JpaRepository<PolicySupport, Long> {

    Optional<PolicySupport> findBySourceAndExternalId(String source, String externalId);

    List<PolicySupport> findAllByIdIn(Collection<Long> ids);

    List<PolicySupport> findTop50ByOrderByUpdatedAtDesc();

    long countByEmbeddingSyncedAtIsNotNull();

    long countByEmbeddingSyncedAtIsNull();

    @Query(
            value = """
                    SELECT *
                    FROM policy_supports policy
                    WHERE (:source IS NULL OR policy.source = :source)
                      AND (:region IS NULL OR LOWER(CAST(COALESCE(policy.region, '') AS text)) LIKE LOWER(CONCAT('%', :region, '%')))
                      AND (:category IS NULL OR LOWER(CAST(COALESCE(policy.category, '') AS text)) LIKE LOWER(CONCAT('%', :category, '%')))
                      AND (
                            :keyword IS NULL
                            OR LOWER(CAST(policy.title AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(CAST(COALESCE(policy.summary, '') AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(CAST(COALESCE(policy.target_group, '') AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    ORDER BY policy.updated_at DESC
                    """,
            countQuery = """
                    SELECT count(*)
                    FROM policy_supports policy
                    WHERE (:source IS NULL OR policy.source = :source)
                      AND (:region IS NULL OR LOWER(CAST(COALESCE(policy.region, '') AS text)) LIKE LOWER(CONCAT('%', :region, '%')))
                      AND (:category IS NULL OR LOWER(CAST(COALESCE(policy.category, '') AS text)) LIKE LOWER(CONCAT('%', :category, '%')))
                      AND (
                            :keyword IS NULL
                            OR LOWER(CAST(policy.title AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(CAST(COALESCE(policy.summary, '') AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(CAST(COALESCE(policy.target_group, '') AS text)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            nativeQuery = true
    )
    Page<PolicySupport> search(
            @Param("source") String source,
            @Param("region") String region,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

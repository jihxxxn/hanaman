package com.hanaman.repository;

import com.hanaman.domain.CycleSummary;
import com.hanaman.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CycleSummaryRepository extends JpaRepository<CycleSummary, UUID> {

    Optional<CycleSummary> findFirstByUserAndAcknowledgedFalseOrderByCycleEndDateDesc(User user);
}

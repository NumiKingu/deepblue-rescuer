package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {
    // Consulta A
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Consulta B
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Consulta C
    List<RescueCase> findByRescueCenterCode(String code);

    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(java.time.LocalDate date);
}
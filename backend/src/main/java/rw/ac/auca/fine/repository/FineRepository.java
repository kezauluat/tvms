package rw.ac.auca.fine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.violation.domain.AppealStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FineRepository extends JpaRepository<Fine, UUID> {

    @Query("select f from Fine f join fetch f.violation v join fetch v.driver join fetch v.vehicle join fetch v.type "
            + "where f.id = :id")
    Optional<Fine> findDetailedById(@Param("id") UUID id);

    /** Unpaid fines past their due date, skipping those whose appeal is still being reviewed. */
    @Query("select f from Fine f join fetch f.violation v "
            + "where f.status = :status and f.dueDate < :today and v.appealStatus <> :pending")
    List<Fine> findOverdueCandidates(@Param("status") FineStatus status,
                                     @Param("today") LocalDate today,
                                     @Param("pending") AppealStatus pending);

    List<Fine> findByStatusAndDueDate(FineStatus status, LocalDate dueDate);

    long countByStatus(FineStatus status);

    @Query("select coalesce(sum(f.amount + f.penaltyAmount), 0) from Fine f where f.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") FineStatus status);
}

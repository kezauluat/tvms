package rw.ac.auca.violation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ViolationRepository extends JpaRepository<Violation, UUID> {


    @Override
    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    Page<Violation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    Page<Violation> findByAppealStatus(AppealStatus appealStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    Page<Violation> findByVehiclePlateNumberContainingIgnoreCase(String plate, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    Page<Violation> findByOfficerId(UUID officerId, Pageable pageable);

    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    @Query("select v from Violation v where v.id = :id")
    Optional<Violation> findDetailedById(@Param("id") UUID id);

    /** Violations the driver committed, plus violations on vehicles the driver owns. */
    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    @Query("select v from Violation v where v.driver.id = :driverId or v.vehicle.owner.id = :driverId order by v.occurredAt desc")
    List<Violation> findForDriverPortal(@Param("driverId") UUID driverId);

    @EntityGraph(attributePaths = {"vehicle", "driver", "officer", "type", "fine"})
    List<Violation> findByVehiclePlateNumberAndStatusOrderByOccurredAtDesc(String plateNumber, ViolationStatus status);

    @Query("select coalesce(sum(v.type.demeritPoints), 0) from Violation v "
            + "where v.driver.id = :driverId and v.status = :status and v.occurredAt >= :since")
    Long sumDemeritPoints(@Param("driverId") UUID driverId,
                          @Param("status") ViolationStatus status,
                          @Param("since") LocalDateTime since);

    @Query("select v.type.name, count(v) from Violation v where v.status = :status "
            + "group by v.type.name order by count(v) desc")
    List<Object[]> countByTypeName(@Param("status") ViolationStatus status);

    long countByAppealStatus(AppealStatus appealStatus);

    boolean existsByReferenceNumber(String referenceNumber);
}

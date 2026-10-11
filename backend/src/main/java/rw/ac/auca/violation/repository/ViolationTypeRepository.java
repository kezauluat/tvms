package rw.ac.auca.violation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.auca.violation.domain.ViolationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ViolationTypeRepository extends JpaRepository<ViolationType, UUID> {
    Optional<ViolationType> findByCode(String code);
    List<ViolationType> findByActiveTrueOrderByNameAsc();
    List<ViolationType> findAllByOrderByNameAsc();
}

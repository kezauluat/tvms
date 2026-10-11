package rw.ac.auca.officer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.officer.domain.Officer;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, UUID> {
    Optional<Officer> findByBadgeNumber(String badgeNumber);
    Optional<Officer> findByUserId(UUID userId);
}

package rw.ac.auca.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.driver.domain.Driver;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Optional<Driver> findByUserId(UUID userId);
}

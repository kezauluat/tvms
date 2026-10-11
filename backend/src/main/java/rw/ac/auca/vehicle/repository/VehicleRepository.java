package rw.ac.auca.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.ac.auca.vehicle.domain.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    Optional<Vehicle> findByPlateNumber(String plateNumber);
    List<Vehicle> findByOwnerIdOrderByPlateNumberAsc(UUID ownerId);
}

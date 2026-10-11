package rw.ac.auca.vehicle.service;

import rw.ac.auca.vehicle.domain.Vehicle;

import java.util.List;
import java.util.UUID;

public interface VehicleService {
    Vehicle registerVehicle(Vehicle vehicle);
    Vehicle updateVehicle(UUID id, Vehicle vehicle);
    void deleteVehicle(UUID id);
    Vehicle findVehicleById(UUID id);
    List<Vehicle> findAllVehicles();
}

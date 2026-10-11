package rw.ac.auca.vehicle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.repository.VehicleRepository;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class VehicleServiceImpl implements VehicleService {

    // Rwanda plate format e.g. RAB123A
    private static final Pattern PLATE_PATTERN = Pattern.compile("^R[A-Z]{2}\\d{3}[A-Z]$");

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Override
    public Vehicle registerVehicle(Vehicle vehicle) {
        vehicle.setId(null);
        vehicle.setPlateNumber(normalizePlate(vehicle.getPlateNumber()));
        validateVehicle(vehicle, null);
        Driver owner = resolveOwner(vehicle);
        vehicle.setOwner(owner);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle updateVehicle(UUID id, Vehicle vehicle) {
        Vehicle found = findVehicleById(id);
        vehicle.setPlateNumber(normalizePlate(vehicle.getPlateNumber()));
        validateVehicle(vehicle, id);
        Driver owner = resolveOwner(vehicle);
        found.setPlateNumber(vehicle.getPlateNumber());
        found.setMake(vehicle.getMake());
        found.setModel(vehicle.getModel());
        found.setYear(vehicle.getYear());
        found.setOwner(owner);
        return vehicleRepository.save(found);
    }

    @Override
    public void deleteVehicle(UUID id) {
        Vehicle found = findVehicleById(id);
        vehicleRepository.delete(found);
    }

    @Override
    public Vehicle findVehicleById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
    }

    @Override
    public List<Vehicle> findAllVehicles() {
        return vehicleRepository.findAll();
    }

    @Override
    public Vehicle findVehicleByPlate(String plateNumber) {
        String plate = normalizePlate(plateNumber);
        return vehicleRepository.findByPlateNumber(plate)
                .orElseThrow(() -> new ResourceNotFoundException("No registered vehicle with plate " + plate));
    }

    /** "rab 123 a" -> "RAB123A" so officers and owners can type plates naturally. */
    public static String normalizePlate(String plateNumber) {
        return plateNumber == null ? null : plateNumber.replaceAll("\\s+", "").toUpperCase();
    }

    private Driver resolveOwner(Vehicle vehicle) {
        if (vehicle.getOwner() == null || vehicle.getOwner().getId() == null) {
            throw new BusinessValidationException("Vehicle must reference an existing driver (owner.id)");
        }
        return driverRepository.findById(vehicle.getOwner().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver (owner) not found"));
    }

    // Business logic validation: plate number format, no future year, unique plate
    private void validateVehicle(Vehicle vehicle, UUID currentId) {
        if (vehicle.getPlateNumber() == null || !PLATE_PATTERN.matcher(vehicle.getPlateNumber()).matches()) {
            throw new BusinessValidationException("Plate number must match format RAB123A");
        }
        if (vehicle.getYear() != null && vehicle.getYear() > Year.now().getValue()) {
            throw new BusinessValidationException("Manufacture year cannot be in the future");
        }
        vehicleRepository.findByPlateNumber(vehicle.getPlateNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new BusinessValidationException(
                        "Plate number already registered: " + vehicle.getPlateNumber());
            }
        });
    }
}

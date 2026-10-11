package rw.ac.auca.driver.dto;

import rw.ac.auca.vehicle.domain.Vehicle;

import java.util.UUID;

public record VehicleSummaryResponse(UUID id, String plateNumber, String make, String model, Integer year) {
    public static VehicleSummaryResponse from(Vehicle v) {
        return new VehicleSummaryResponse(v.getId(), v.getPlateNumber(), v.getMake(), v.getModel(), v.getYear());
    }
}

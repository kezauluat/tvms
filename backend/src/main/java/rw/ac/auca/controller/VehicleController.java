package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.service.VehicleService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @ResponseStatus(HttpStatus.OK)
    public List<Vehicle> getAllVehicles() {
        return vehicleService.findAllVehicles();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @ResponseStatus(HttpStatus.OK)
    public Vehicle getVehicleById(@PathVariable UUID id) {
        return vehicleService.findVehicleById(id);
    }

    /** Officers look a vehicle up by plate at the roadside, e.g. GET /api/vehicles/plate/RAB123A */
    @GetMapping("/plate/{plateNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @ResponseStatus(HttpStatus.OK)
    public Vehicle getVehicleByPlate(@PathVariable String plateNumber) {
        return vehicleService.findVehicleByPlate(plateNumber);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return vehicleService.registerVehicle(vehicle);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public Vehicle updateVehicle(@PathVariable UUID id, @Valid @RequestBody Vehicle vehicle) {
        return vehicleService.updateVehicle(id, vehicle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
    }
}

package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public List<Vehicle> getAllVehicles() {
        return vehicleService.findAllVehicles();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Vehicle getVehicleById(@PathVariable UUID id) {
        return vehicleService.findVehicleById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle createVehicle(@Valid @RequestBody Vehicle vehicle) {
        return vehicleService.registerVehicle(vehicle);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Vehicle updateVehicle(@PathVariable UUID id, @Valid @RequestBody Vehicle vehicle) {
        return vehicleService.updateVehicle(id, vehicle);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
    }
}

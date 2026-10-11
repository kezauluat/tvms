package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.service.DriverService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Driver> getAllDrivers() {
        return driverService.findAllDrivers();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Driver getDriverById(@PathVariable UUID id) {
        return driverService.findDriverById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Driver createDriver(@Valid @RequestBody Driver driver) {
        return driverService.registerDriver(driver);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Driver updateDriver(@PathVariable UUID id, @Valid @RequestBody Driver driver) {
        return driverService.updateDriver(id, driver);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDriver(@PathVariable UUID id) {
        driverService.deleteDriver(id);
    }
}

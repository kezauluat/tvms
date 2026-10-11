package rw.ac.auca.driver.service;

import rw.ac.auca.driver.domain.Driver;

import java.util.List;
import java.util.UUID;

public interface DriverService {
    Driver registerDriver(Driver driver);
    Driver updateDriver(UUID id, Driver driver);
    void deleteDriver(UUID id);
    Driver findDriverById(UUID id);
    List<Driver> findAllDrivers();
}

package rw.ac.auca.driver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    public Driver registerDriver(Driver driver) {
        driver.setId(null); // never let a client overwrite an existing record through POST
        driver.setLicenseNumber(normalizeLicense(driver.getLicenseNumber()));
        validateDriver(driver, null);
        return driverRepository.save(driver);
    }

    @Override
    public Driver updateDriver(UUID id, Driver driver) {
        Driver found = findDriverById(id);
        driver.setLicenseNumber(normalizeLicense(driver.getLicenseNumber()));
        validateDriver(driver, id);
        found.setFullName(driver.getFullName());
        found.setLicenseNumber(driver.getLicenseNumber());
        found.setDateOfBirth(driver.getDateOfBirth());
        found.setPhoneNumber(driver.getPhoneNumber());
        found.setAddress(driver.getAddress());
        return driverRepository.save(found);
    }

    @Override
    public void deleteDriver(UUID id) {
        Driver found = findDriverById(id);
        driverRepository.delete(found);
    }

    @Override
    public Driver findDriverById(UUID id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
    }

    @Override
    public List<Driver> findAllDrivers() {
        return driverRepository.findAll();
    }

    private static String normalizeLicense(String licenseNumber) {
        return licenseNumber == null ? null : licenseNumber.trim().toUpperCase();
    }

    // Business logic validation: minimum driving age + unique license number
    private void validateDriver(Driver driver, UUID currentId) {
        if (driver.getDateOfBirth() == null
                || Period.between(driver.getDateOfBirth(), LocalDate.now()).getYears() < 18) {
            throw new BusinessValidationException("Driver must be at least 18 years old");
        }
        driverRepository.findByLicenseNumber(driver.getLicenseNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new BusinessValidationException(
                        "License number already registered: " + driver.getLicenseNumber());
            }
        });
    }
}

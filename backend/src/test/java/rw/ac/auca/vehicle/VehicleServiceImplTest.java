package rw.ac.auca.vehicle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.vehicle.service.VehicleServiceImpl;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock DriverRepository driverRepository;
    @InjectMocks VehicleServiceImpl service;

    private Vehicle vehicle(String plate, int year) {
        Vehicle v = new Vehicle();
        v.setPlateNumber(plate);
        v.setMake("Toyota");
        v.setModel("RAV4");
        v.setYear(year);
        return v;
    }

    @Test
    void normalizesPlateTypedWithSpaces() {
        assertThat(VehicleServiceImpl.normalizePlate("rab 123 a")).isEqualTo("RAB123A");
    }

    @Test
    void rejectsInvalidPlateFormat() {
        assertThatThrownBy(() -> service.registerVehicle(vehicle("ABC-1", 2020)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("RAB123A");
    }

    @Test
    void rejectsFutureManufactureYear() {
        assertThatThrownBy(() -> service.registerVehicle(vehicle("RAB123A", Year.now().getValue() + 1)))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("future");
    }
}

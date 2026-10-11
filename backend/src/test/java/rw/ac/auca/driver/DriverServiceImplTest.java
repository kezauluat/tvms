package rw.ac.auca.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.driver.service.DriverServiceImpl;
import rw.ac.auca.exception.BusinessValidationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock DriverRepository driverRepository;
    @InjectMocks DriverServiceImpl service;

    private Driver driver(LocalDate dob) {
        Driver d = new Driver();
        d.setFullName("Jean Mugisha");
        d.setLicenseNumber(" rw-dl-9 ");
        d.setDateOfBirth(dob);
        d.setPhoneNumber("0788000000");
        return d;
    }

    @Test
    void registersAdultDriverAndNormalizesLicense() {
        when(driverRepository.findByLicenseNumber("RW-DL-9")).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        Driver saved = service.registerDriver(driver(LocalDate.now().minusYears(30)));

        assertThat(saved.getLicenseNumber()).isEqualTo("RW-DL-9");
    }

    @Test
    void rejectsDriverUnder18() {
        assertThatThrownBy(() -> service.registerDriver(driver(LocalDate.now().minusYears(17))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("18");
        verify(driverRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateLicense() {
        Driver existing = driver(LocalDate.now().minusYears(40));
        existing.setId(UUID.randomUUID());
        when(driverRepository.findByLicenseNumber("RW-DL-9")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerDriver(driver(LocalDate.now().minusYears(30))))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already registered");
    }
}

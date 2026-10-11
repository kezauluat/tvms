package rw.ac.auca.driver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.dto.DriverSummaryResponse;
import rw.ac.auca.driver.dto.VehicleSummaryResponse;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.ForbiddenException;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationStatus;
import rw.ac.auca.violation.repository.ViolationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverPortalService {

    /** Demerit points counted over the last 12 months; reaching the limit puts the license at risk. */
    public static final int DEMERIT_POINT_LIMIT = 20;

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ViolationRepository violationRepository;

    @Transactional(readOnly = true)
    public DriverSummaryResponse summary(AuthUser user) {
        Driver driver = currentDriver(user);
        List<Violation> violations = violationRepository.findForDriverPortal(driver.getId());
        List<Fine> unpaid = violations.stream()
                .map(Violation::getFine)
                .filter(f -> f != null && f.isPayable())
                .toList();
        BigDecimal outstanding = unpaid.stream().map(Fine::totalDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        Long points = violationRepository.sumDemeritPoints(driver.getId(), ViolationStatus.ACTIVE,
                LocalDateTime.now().minusMonths(12));
        long demeritPoints = points == null ? 0 : points;
        return new DriverSummaryResponse(driver.getId(), driver.getFullName(), driver.getLicenseNumber(),
                driver.getPhoneNumber(), vehicleRepository.findByOwnerIdOrderByPlateNumberAsc(driver.getId()).size(),
                unpaid.size(), outstanding, demeritPoints, DEMERIT_POINT_LIMIT, demeritPoints >= DEMERIT_POINT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<VehicleSummaryResponse> vehicles(AuthUser user) {
        Driver driver = currentDriver(user);
        return vehicleRepository.findByOwnerIdOrderByPlateNumberAsc(driver.getId()).stream()
                .map(VehicleSummaryResponse::from).toList();
    }

    private Driver currentDriver(AuthUser user) {
        return driverRepository.findByUserId(user.id())
                .orElseThrow(() -> new ForbiddenException("Complete your driver profile first"));
    }
}

package rw.ac.auca.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.driver.dto.DriverSummaryResponse;
import rw.ac.auca.driver.dto.VehicleSummaryResponse;
import rw.ac.auca.driver.service.DriverPortalService;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.violation.dto.ViolationResponse;
import rw.ac.auca.violation.service.ViolationService;

import java.util.List;

/** Driver portal: everything about the logged-in driver. */
@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
public class MeController {

    private final DriverPortalService driverPortalService;
    private final ViolationService violationService;

    @GetMapping("/summary")
    public DriverSummaryResponse summary(@AuthenticationPrincipal AuthUser user) {
        return driverPortalService.summary(user);
    }

    @GetMapping("/vehicles")
    public List<VehicleSummaryResponse> vehicles(@AuthenticationPrincipal AuthUser user) {
        return driverPortalService.vehicles(user);
    }

    @GetMapping("/violations")
    public List<ViolationResponse> violations(@AuthenticationPrincipal AuthUser user) {
        return violationService.forDriver(user);
    }
}

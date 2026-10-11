package rw.ac.auca.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.admin.dto.AdminStatsResponse;
import rw.ac.auca.admin.dto.AdminUserResponse;
import rw.ac.auca.admin.dto.CreateOfficerAccountRequest;
import rw.ac.auca.audit.AuditService;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.auth.repository.UserRepository;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ConflictException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.repository.OfficerRepository;
import rw.ac.auca.officer.service.OfficerService;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.domain.ViolationStatus;
import rw.ac.auca.violation.repository.ViolationRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OfficerRepository officerRepository;
    private final OfficerService officerService;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ViolationRepository violationRepository;
    private final FineRepository fineRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /** Dashboard numbers. Cached in Redis; every write that changes them clears the cache. */
    @Cacheable("adminStats")
    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        List<AdminStatsResponse.TypeCount> byType = violationRepository.countByTypeName(ViolationStatus.ACTIVE).stream()
                .map(row -> new AdminStatsResponse.TypeCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
        return new AdminStatsResponse(
                violationRepository.count(),
                violationRepository.countByAppealStatus(AppealStatus.PENDING),
                fineRepository.countByStatus(FineStatus.UNPAID),
                fineRepository.countByStatus(FineStatus.OVERDUE),
                fineRepository.sumTotalByStatus(FineStatus.UNPAID).add(fineRepository.sumTotalByStatus(FineStatus.OVERDUE)),
                fineRepository.sumTotalByStatus(FineStatus.PAID),
                driverRepository.count(),
                vehicleRepository.count(),
                officerRepository.count(),
                byType);
    }

    @CacheEvict(cacheNames = "adminStats", allEntries = true)
    @Transactional
    public Officer createOfficerAccount(AuthUser admin, CreateOfficerAccountRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = userRepository.save(User.builder()
                .fullName(req.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.OFFICER)
                .build());
        Officer officer = new Officer();
        officer.setFullName(req.fullName().trim());
        officer.setBadgeNumber(req.badgeNumber().trim().toUpperCase());
        officer.setStation(req.station());
        officer.setPhoneNumber(req.phoneNumber());
        officer = officerService.registerOfficer(officer);   // unique badge rule lives in OfficerServiceImpl
        officer.setUser(user);
        auditService.log(admin.id(), "CREATE_OFFICER_ACCOUNT", "Officer", officer.getId());
        return officerRepository.save(officer);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> users() {
        return userRepository.findAll(Sort.by("role", "fullName")).stream().map(AdminUserResponse::from).toList();
    }

    @Transactional
    public AdminUserResponse setEnabled(AuthUser admin, UUID userId, boolean enabled) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessValidationException("Administrator accounts cannot be disabled here");
        }
        user.setEnabled(enabled);
        auditService.log(admin.id(), enabled ? "ENABLE_USER" : "DISABLE_USER", "User", user.getId());
        return AdminUserResponse.from(user);
    }
}

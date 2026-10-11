package rw.ac.auca.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.auth.repository.UserRepository;
import rw.ac.auca.base.ReferenceGenerator;
import rw.ac.auca.document.ViolationEvidence;
import rw.ac.auca.document.repository.ViolationEvidenceRepository;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.driver.repository.DriverRepository;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.fine.repository.FineRepository;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.repository.OfficerRepository;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.vehicle.repository.VehicleRepository;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationType;
import rw.ac.auca.violation.repository.ViolationRepository;
import rw.ac.auca.violation.repository.ViolationTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Seeds an admin account and, on an empty database, demo data so every screen has something to show.
 * Fine amounts are sample values for the project, not official Rwanda National Police tariffs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final OfficerRepository officerRepository;
    private final VehicleRepository vehicleRepository;
    private final ViolationTypeRepository violationTypeRepository;
    private final ViolationRepository violationRepository;
    private final FineRepository fineRepository;
    private final ViolationEvidenceRepository evidenceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        ensureUser("System Administrator", adminEmail, adminPassword, Role.ADMIN);
        if (violationTypeRepository.count() > 0) {
            return;
        }

        ViolationType speeding = type("SPEEDING", "Speeding", "Driving above the posted speed limit", 25000, 4);
        type("RED_LIGHT", "Running a red light", "Crossing a junction on a red signal", 25000, 5);
        ViolationType seatbelt = type("NO_SEATBELT", "No seatbelt", "Driver or passenger not wearing a seatbelt", 10000, 2);
        ViolationType phone = type("PHONE_USE", "Using a phone while driving", "Holding a phone while the vehicle is moving", 10000, 3);
        type("DRUNK_DRIVING", "Driving under the influence", "Alcohol level above the legal limit", 150000, 10);
        type("NO_INSURANCE", "No valid insurance", "Vehicle insurance missing or expired", 30000, 0);
        type("ILLEGAL_PARKING", "Illegal parking", "Parking in a prohibited place", 10000, 0);

        User officerUser = ensureUser("Insp. Eric Habimana", "officer@tvms.rw", "Officer@12345", Role.OFFICER);
        Officer officer = new Officer();
        officer.setBadgeNumber("RNP-1001");
        officer.setFullName("Insp. Eric Habimana");
        officer.setStation("Kicukiro Traffic Unit");
        officer.setPhoneNumber("0788100200");
        officer.setUser(officerUser);
        officer = officerRepository.save(officer);

        // Driver with an online account
        User driverUser = ensureUser("Aline Uwimana", "driver@tvms.rw", "Driver@12345", Role.DRIVER);
        Driver aline = driver("Aline Uwimana", "RW-DL-100001", LocalDate.of(1994, 3, 15), "0788300400", "Kigali, Gasabo");
        aline.setUser(driverUser);
        aline = driverRepository.save(aline);

        // Driver registered by an admin, without an account yet (can claim it on the Register page)
        Driver jean = driverRepository.save(driver("Jean Mugisha", "RW-DL-200002", LocalDate.of(1990, 7, 1),
                "0722500600", "Kigali, Nyarugenge"));

        Vehicle corolla = vehicle("RAB123A", "Toyota", "Corolla", 2018, aline);
        Vehicle rav4 = vehicle("RAD456B", "Toyota", "RAV4", 2021, jean);

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Violation v1 = violation(corolla, aline, officer, speeding, "KN 5 Rd, Remera", now.minusDays(3), FineStatus.UNPAID);
        violation(corolla, aline, officer, seatbelt, "KG 11 Ave, Kimironko", now.minusDays(45), FineStatus.PAID);
        violation(rav4, jean, officer, phone, "KN 3 Rd, Nyamirambo", now.minusDays(1), FineStatus.UNPAID);

        try {
            evidenceRepository.save(ViolationEvidence.builder()
                    .violationId(v1.getId().toString())
                    .violationReference(v1.getReferenceNumber())
                    .officerBadge(officer.getBadgeNumber())
                    .notes("Speed camera reading confirmed by radar gun. Driver cooperative.")
                    .latitude(-1.9536)
                    .longitude(30.1044)
                    .details(Map.of("recordedSpeedKmh", "92", "speedLimitKmh", "60", "device", "Radar TR-7"))
                    .photoUrls(List.of())
                    .build());
        } catch (Exception e) {
            log.warn("Could not seed evidence in MongoDB: {}", e.getMessage());
        }
        log.info("Seeded demo data: 7 violation types, 1 officer, 2 drivers, 2 vehicles, 3 violations");
    }

    private User ensureUser(String name, String email, String password, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build()));
    }

    private ViolationType type(String code, String name, String description, long amount, int points) {
        return violationTypeRepository.save(ViolationType.builder()
                .code(code).name(name).description(description)
                .fineAmount(BigDecimal.valueOf(amount)).demeritPoints(points)
                .build());
    }

    private Driver driver(String name, String license, LocalDate dob, String phone, String address) {
        Driver d = new Driver();
        d.setFullName(name);
        d.setLicenseNumber(license);
        d.setDateOfBirth(dob);
        d.setPhoneNumber(phone);
        d.setAddress(address);
        return d;
    }

    private Vehicle vehicle(String plate, String make, String model, int year, Driver owner) {
        Vehicle v = new Vehicle();
        v.setPlateNumber(plate);
        v.setMake(make);
        v.setModel(model);
        v.setYear(year);
        v.setOwner(owner);
        return vehicleRepository.save(v);
    }

    private Violation violation(Vehicle vehicle, Driver driver, Officer officer, ViolationType type,
                                String location, LocalDateTime when, FineStatus fineStatus) {
        Violation v = violationRepository.save(Violation.builder()
                .referenceNumber(ReferenceGenerator.violationReference(when.toLocalDate()))
                .vehicle(vehicle).driver(driver).officer(officer).type(type)
                .location(location).occurredAt(when)
                .build());
        Fine fine = Fine.builder()
                .violation(v)
                .amount(type.getFineAmount())
                .dueDate(when.toLocalDate().plusDays(30))
                .status(fineStatus)
                .build();
        if (fineStatus == FineStatus.PAID) {
            fine.setPaidAt(when.plusDays(2));
            fine.setPayerPhone(driver.getPhoneNumber());
            fine.setPaymentReference(ReferenceGenerator.paymentReference());
        }
        v.setFine(fineRepository.save(fine));
        return v;
    }
}

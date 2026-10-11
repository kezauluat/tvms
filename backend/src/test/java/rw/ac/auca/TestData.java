package rw.ac.auca;

import rw.ac.auca.auth.domain.Role;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.driver.domain.Driver;
import rw.ac.auca.fine.domain.Fine;
import rw.ac.auca.fine.domain.FineStatus;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.vehicle.domain.Vehicle;
import rw.ac.auca.violation.domain.Violation;
import rw.ac.auca.violation.domain.ViolationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Small object factory shared by the unit tests. */
public final class TestData {

    private TestData() {}

    public static User user(Role role) {
        User u = User.builder().fullName("Test " + role).email(role.name().toLowerCase() + "@test.rw").role(role).build();
        u.setId(UUID.randomUUID());
        return u;
    }

    public static Driver driver(User user) {
        Driver d = new Driver();
        d.setId(UUID.randomUUID());
        d.setFullName("Aline Uwimana");
        d.setLicenseNumber("RW-DL-100001");
        d.setDateOfBirth(LocalDate.of(1994, 3, 15));
        d.setPhoneNumber("0788300400");
        d.setUser(user);
        return d;
    }

    public static Officer officer(User user) {
        Officer o = new Officer();
        o.setId(UUID.randomUUID());
        o.setFullName("Insp. Test");
        o.setBadgeNumber("RNP-1");
        o.setPhoneNumber("0788100200");
        o.setUser(user);
        return o;
    }

    public static Vehicle vehicle(Driver owner) {
        Vehicle v = new Vehicle();
        v.setId(UUID.randomUUID());
        v.setPlateNumber("RAB123A");
        v.setMake("Toyota");
        v.setModel("Corolla");
        v.setYear(2018);
        v.setOwner(owner);
        return v;
    }

    public static ViolationType speeding() {
        ViolationType t = ViolationType.builder().code("SPEEDING").name("Speeding")
                .fineAmount(new BigDecimal("25000")).demeritPoints(4).build();
        t.setId(UUID.randomUUID());
        return t;
    }

    public static Violation violationWithFine(Vehicle vehicle, Driver driver, Officer officer, LocalDateTime when, FineStatus status) {
        Violation v = Violation.builder().referenceNumber("TV261011-TEST1").vehicle(vehicle).driver(driver)
                .officer(officer).type(speeding()).location("KN 5 Rd").occurredAt(when).build();
        v.setId(UUID.randomUUID());
        Fine f = Fine.builder().violation(v).amount(new BigDecimal("25000")).dueDate(LocalDate.now().plusDays(10))
                .status(status).build();
        f.setId(UUID.randomUUID());
        v.setFine(f);
        return v;
    }
}

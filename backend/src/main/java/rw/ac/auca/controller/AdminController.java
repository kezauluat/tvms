package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.admin.dto.AdminStatsResponse;
import rw.ac.auca.admin.dto.AdminUserResponse;
import rw.ac.auca.admin.dto.CreateOfficerAccountRequest;
import rw.ac.auca.admin.service.AdminService;
import rw.ac.auca.fine.service.FineService;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.security.AuthUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FineService fineService;

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @PostMapping("/officer-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Officer createOfficerAccount(@AuthenticationPrincipal AuthUser user,
                                        @Valid @RequestBody CreateOfficerAccountRequest request) {
        return adminService.createOfficerAccount(user, request);
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.users();
    }

    @PatchMapping("/users/{id}/enabled")
    public AdminUserResponse setEnabled(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                        @RequestParam boolean value) {
        return adminService.setEnabled(user, id, value);
    }

    /** Runs the nightly jobs immediately (useful for demos): overdue penalties + due reminders. */
    @PostMapping("/jobs/run-fine-checks")
    public Map<String, Integer> runFineChecks() {
        return Map.of("markedOverdue", fineService.markOverdueFines(),
                "remindersQueued", fineService.sendDueReminders());
    }
}

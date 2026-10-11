package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.base.PageResponse;
import rw.ac.auca.security.AuthUser;
import rw.ac.auca.violation.domain.AppealStatus;
import rw.ac.auca.violation.dto.*;
import rw.ac.auca.violation.service.ViolationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationService violationService;

    @PostMapping
    @PreAuthorize("hasRole('OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ViolationResponse record(@AuthenticationPrincipal AuthUser user,
                                    @Valid @RequestBody RecordViolationRequest request) {
        return violationService.record(user, request);
    }

    @GetMapping("/recorded-by-me")
    @PreAuthorize("hasRole('OFFICER')")
    public PageResponse<ViolationResponse> recordedByMe(@AuthenticationPrincipal AuthUser user,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return violationService.recordedByMe(user, page, size);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ViolationResponse> search(@RequestParam(required = false) String plate,
                                                  @RequestParam(required = false) AppealStatus appealStatus,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return violationService.search(plate, appealStatus, page, size);
    }

    /** Any role; drivers only get their own violations (checked in the service). */
    @GetMapping("/{id}")
    public ViolationDetailResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return violationService.get(user, id);
    }

    @PostMapping("/{id}/appeal")
    @PreAuthorize("hasRole('DRIVER')")
    public ViolationResponse appeal(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                    @Valid @RequestBody AppealRequest request) {
        return violationService.appeal(user, id, request);
    }

    @PatchMapping("/{id}/appeal-decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ViolationResponse decideAppeal(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                                          @Valid @RequestBody AppealDecisionRequest request) {
        return violationService.decideAppeal(user, id, request);
    }
}

package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.fine.dto.PayFineRequest;
import rw.ac.auca.fine.dto.PaymentResponse;
import rw.ac.auca.fine.service.FineService;
import rw.ac.auca.security.AuthUser;

import java.util.UUID;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('DRIVER')")
    public PaymentResponse pay(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id,
                               @Valid @RequestBody PayFineRequest request) {
        return fineService.pay(user, id, request);
    }
}

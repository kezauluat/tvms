package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.violation.dto.ViolationTypeRequest;
import rw.ac.auca.violation.dto.ViolationTypeResponse;
import rw.ac.auca.violation.service.ViolationTypeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/violation-types")
@RequiredArgsConstructor
public class ViolationTypeController {

    private final ViolationTypeService violationTypeService;

    /** Active types, cached in Redis. Used by the officer's "record violation" form. */
    @GetMapping
    public List<ViolationTypeResponse> active() {
        return violationTypeService.listActive();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ViolationTypeResponse> all() {
        return violationTypeService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ViolationTypeResponse create(@Valid @RequestBody ViolationTypeRequest request) {
        return violationTypeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ViolationTypeResponse update(@PathVariable UUID id, @Valid @RequestBody ViolationTypeRequest request) {
        return violationTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id) {
        violationTypeService.deactivate(id);
    }
}

package rw.ac.auca.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.service.OfficerService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/officers")
public class OfficerController {

    private final OfficerService officerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public List<Officer> getAllOfficers() {
        return officerService.findAllOfficers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public Officer getOfficerById(@PathVariable UUID id) {
        return officerService.findOfficerById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Officer createOfficer(@Valid @RequestBody Officer officer) {
        return officerService.registerOfficer(officer);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public Officer updateOfficer(@PathVariable UUID id, @Valid @RequestBody Officer officer) {
        return officerService.updateOfficer(id, officer);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOfficer(@PathVariable UUID id) {
        officerService.deleteOfficer(id);
    }
}

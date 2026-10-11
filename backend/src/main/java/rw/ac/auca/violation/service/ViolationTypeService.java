package rw.ac.auca.violation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.auca.exception.ConflictException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.violation.domain.ViolationType;
import rw.ac.auca.violation.dto.ViolationTypeRequest;
import rw.ac.auca.violation.dto.ViolationTypeResponse;
import rw.ac.auca.violation.repository.ViolationTypeRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViolationTypeService {

    private final ViolationTypeRepository violationTypeRepository;

    /** Read on every officer form load, so it is cached in Redis and cleared on any change. */
    @Cacheable("violationTypes")
    @Transactional(readOnly = true)
    public List<ViolationTypeResponse> listActive() {
        return violationTypeRepository.findByActiveTrueOrderByNameAsc().stream().map(ViolationTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ViolationTypeResponse> listAll() {
        return violationTypeRepository.findAllByOrderByNameAsc().stream().map(ViolationTypeResponse::from).toList();
    }

    @CacheEvict(cacheNames = "violationTypes", allEntries = true)
    @Transactional
    public ViolationTypeResponse create(ViolationTypeRequest req) {
        String code = req.code().trim().toUpperCase();
        if (violationTypeRepository.findByCode(code).isPresent()) {
            throw new ConflictException("A violation type with code " + code + " already exists");
        }
        ViolationType type = ViolationType.builder()
                .code(code)
                .name(req.name().trim())
                .description(req.description())
                .fineAmount(req.fineAmount())
                .demeritPoints(req.demeritPoints())
                .active(req.active() == null || req.active())
                .build();
        return ViolationTypeResponse.from(violationTypeRepository.save(type));
    }

    /** Changing the amount only affects new fines: issued fines keep the amount copied at issue time. */
    @CacheEvict(cacheNames = "violationTypes", allEntries = true)
    @Transactional
    public ViolationTypeResponse update(UUID id, ViolationTypeRequest req) {
        ViolationType type = find(id);
        String code = req.code().trim().toUpperCase();
        violationTypeRepository.findByCode(code).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A violation type with code " + code + " already exists");
            }
        });
        type.setCode(code);
        type.setName(req.name().trim());
        type.setDescription(req.description());
        type.setFineAmount(req.fineAmount());
        type.setDemeritPoints(req.demeritPoints());
        if (req.active() != null) {
            type.setActive(req.active());
        }
        return ViolationTypeResponse.from(type);
    }

    /** Soft delete: old violations still reference the type, so it is deactivated, not removed. */
    @CacheEvict(cacheNames = "violationTypes", allEntries = true)
    @Transactional
    public void deactivate(UUID id) {
        find(id).setActive(false);
    }

    private ViolationType find(UUID id) {
        return violationTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Violation type not found with id: " + id));
    }
}

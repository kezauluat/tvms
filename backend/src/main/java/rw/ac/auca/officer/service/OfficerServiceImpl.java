package rw.ac.auca.officer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rw.ac.auca.exception.BusinessValidationException;
import rw.ac.auca.exception.ResourceNotFoundException;
import rw.ac.auca.officer.domain.Officer;
import rw.ac.auca.officer.repository.OfficerRepository;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OfficerServiceImpl implements OfficerService {

    private final OfficerRepository officerRepository;

    @Override
    public Officer registerOfficer(Officer officer) {
        officer.setId(null);
        validateOfficer(officer, null);
        return officerRepository.save(officer);
    }

    @Override
    public Officer updateOfficer(UUID id, Officer officer) {
        Officer found = findOfficerById(id);
        validateOfficer(officer, id);
        found.setBadgeNumber(officer.getBadgeNumber());
        found.setFullName(officer.getFullName());
        found.setStation(officer.getStation());
        found.setPhoneNumber(officer.getPhoneNumber());
        return officerRepository.save(found);
    }

    @Override
    public void deleteOfficer(UUID id) {
        Officer found = findOfficerById(id);
        officerRepository.delete(found);
    }

    @Override
    public Officer findOfficerById(UUID id) {
        return officerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with id: " + id));
    }

    @Override
    public List<Officer> findAllOfficers() {
        return officerRepository.findAll();
    }

    // Business logic validation: unique badge number
    private void validateOfficer(Officer officer, UUID currentId) {
        officerRepository.findByBadgeNumber(officer.getBadgeNumber()).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new BusinessValidationException(
                        "Badge number already registered: " + officer.getBadgeNumber());
            }
        });
    }
}

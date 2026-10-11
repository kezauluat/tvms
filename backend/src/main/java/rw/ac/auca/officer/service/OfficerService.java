package rw.ac.auca.officer.service;

import rw.ac.auca.officer.domain.Officer;

import java.util.List;
import java.util.UUID;

public interface OfficerService {
    Officer registerOfficer(Officer officer);
    Officer updateOfficer(UUID id, Officer officer);
    void deleteOfficer(UUID id);
    Officer findOfficerById(UUID id);
    List<Officer> findAllOfficers();
}

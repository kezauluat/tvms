package rw.ac.auca.document.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.document.ViolationEvidence;

import java.util.Optional;

public interface ViolationEvidenceRepository extends MongoRepository<ViolationEvidence, String> {
    Optional<ViolationEvidence> findByViolationId(String violationId);
}

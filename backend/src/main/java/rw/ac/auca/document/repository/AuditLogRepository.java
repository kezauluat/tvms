package rw.ac.auca.document.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.document.AuditLog;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop50ByOrderByTimestampDesc();
}

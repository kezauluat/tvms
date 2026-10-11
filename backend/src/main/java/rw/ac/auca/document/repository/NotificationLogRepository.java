package rw.ac.auca.document.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import rw.ac.auca.document.NotificationLog;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, String> {
}

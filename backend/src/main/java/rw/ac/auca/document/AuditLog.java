package rw.ac.auca.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Who did what and when. Old entries expire automatically after 180 days (TTL index). */
@Document("audit_logs")
@CompoundIndex(name = "user_time", def = "{'userId': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id;

    private String userId;
    private String action;
    private String entity;
    private String entityId;
    private String ip;

    @Indexed(expireAfter = "180d")
    @Builder.Default
    private Instant timestamp = Instant.now();
}

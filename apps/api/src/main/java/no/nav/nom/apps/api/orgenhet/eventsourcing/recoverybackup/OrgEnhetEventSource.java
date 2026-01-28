package no.nav.nom.apps.api.orgenhet.eventsourcing.recoverybackup;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import tools.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Builder
@Table(name = "T_ORGENHET_EVENTSOURCE")
@AllArgsConstructor
@NoArgsConstructor
public class OrgEnhetEventSource {
    @Id
    @Column(name = "EVENT_KEY", nullable = false, updatable = false)
    private String eventKey;

    @Type(JsonBinaryType.class)
    @Column(name = "PAYLOAD", nullable = false, updatable = false)
    private JsonNode payload;
}

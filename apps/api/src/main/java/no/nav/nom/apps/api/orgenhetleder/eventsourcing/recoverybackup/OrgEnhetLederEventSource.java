package no.nav.nom.apps.api.orgenhetleder.eventsourcing.recoverybackup;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Builder
@Table(name = "T_ORGENHET_LEDER_EVENTSOURCE")
@AllArgsConstructor
@NoArgsConstructor
public class OrgEnhetLederEventSource {
    @Id
    @Column(name = "EVENT_KEY", nullable = false,  updatable = false)
    private String eventKey;

    @Type(JsonBinaryType.class)
    @Column(name = "PAYLOAD",  nullable = false, updatable = false)
    private JsonNode payload;
}

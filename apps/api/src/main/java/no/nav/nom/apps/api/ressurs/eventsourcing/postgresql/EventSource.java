package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Builder
@Table(name = "T_RESSURS_EVENTSOURCE")
@AllArgsConstructor
@NoArgsConstructor
@TypeDefs({
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class EventSource {
    @Id
    @Column(name = "EVENT_KEY", nullable = false, updatable = false)
    private String eventKey;

    @Type(type = "jsonb")
    @Column(name = "PAYLOAD", nullable = false, updatable = false)
    private JsonNode payload;
}

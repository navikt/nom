package no.nav.nom.apps.api.repository.ordre;

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
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "T_HENDELSER")
@AllArgsConstructor
@NoArgsConstructor
@TypeDefs({
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class Ordre {
    @Id
    @Type(type = "pg-uuid")
    @Column(name = "ORDRE_ID", nullable = false, updatable = false)
    private UUID ordreId;

    @Type(type = "jsonb")
    @Column(name = "DATA", nullable = false)
    private JsonNode data;
}

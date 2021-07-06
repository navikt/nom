package no.nav.nom.apps.api.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
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
public class Hendelse {

    @Id
    @Type(type = "pg-uuid")
    @Column(name = "HENDELSE_ID", nullable = false, updatable = false)
    private UUID hendelseID;

    @Type(type = "pg-uuid")
    @Column(name = "FUNKSJONELL_ID", nullable = false, updatable = false)
    private UUID funksjonellId;

    @Type(type = "pg-uuid")
    @Column(name = "FORESPORSEL_ID", nullable = false, updatable = false)
    private UUID foresporselId;

    @Type(type = "pg-uuid")
    @Column(name = "TIDLIGERE_ID", updatable = false)
    private UUID tidligereId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "DATA_TYPE", nullable = false, updatable = false)
    private DataType dataType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "HENDELSE_TYPE", nullable = false, updatable = false)
    private HendelseType hendelseType;

    @Type(type = "jsonb")
    @Column(name = "DATA", nullable = false, updatable = false)
    private JsonNode data;

    @NotNull
    @Column(name = "OPPRETTET_TIDSPUNKT", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    public enum HendelseType {
        OPPRETTET, ENDRET, ANNULLERT
    }

    @Getter
    @AllArgsConstructor
    public enum DataType {
        OPERASJON_V1("no.nav.nom.hendelser:Operasjon:v1"),
        RESSURS_V1("no.nav.nom.hendelser:Ressurs:v1"),
        ORGENHET_V1("no.nav.nom.hendelser:Organisasjonsenhet:v1");

        String key;
    }
}

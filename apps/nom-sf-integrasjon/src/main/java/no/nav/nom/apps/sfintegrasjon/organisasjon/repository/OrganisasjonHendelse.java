package no.nav.nom.apps.sfintegrasjon.organisasjon.repository;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

@Entity
@Table(name = "organisasjon_hendelse")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OrganisasjonHendelse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "mottatt_tidspunkt", nullable = false)
    private OffsetDateTime mottattTidspunkt;

    @Column(name = "fid", nullable = false)
    private String fid;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(name = "orgenhet_id", nullable = false)
    private String orgEnhetId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Type(JsonBinaryType.class)
    @Column(name = "melding", columnDefinition = "jsonb", nullable = false)
    private String melding;

    @Column(name = "behandlings_id")
    private Long behandlingsId;
}

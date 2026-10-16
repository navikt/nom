package no.nav.nom.apps.sfintegrasjon.outbox.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "outbox")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder(toBuilder = true)
public class SfOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sporings_id")
    private Long sporingsId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sf_status")
    private SfStatus sfStatus = SfStatus.IKKE_SENDT;

    @Column(name = "feilmelding")
    private String feilmelding;

    @Column(name = "orgenhet_id")
    private String orgEnhetId;

    @Column(name = "endret_av")
    private Set<String> endretAv;

    @Column(name = "endringshendelser")
    private Set<String> endringshendelser;

    @Type(JsonBinaryType.class)
    @Column(name = "orgenhet", columnDefinition = "jsonb", nullable = false)
    private String orgenhet;
}

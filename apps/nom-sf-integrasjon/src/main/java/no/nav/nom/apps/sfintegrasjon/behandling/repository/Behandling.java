package no.nav.nom.apps.sfintegrasjon.behandling.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "behandling")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "behandlings_id", nullable = false)
    private Long behandlingsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BehandlingStatus status;

    @Column(name = "behandling_tidspunkt")
    private LocalDateTime behandlingTidspunkt;
}

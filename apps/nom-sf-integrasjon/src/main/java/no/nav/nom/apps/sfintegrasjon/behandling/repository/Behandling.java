package no.nav.nom.apps.sfintegrasjon.behandling.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "behandling")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
public class Behandling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "behandlings_id", nullable = false)
    private Long behandlingsId;

    @Column(name = "outbox_sporings_id")
    private Long outboxSporingsId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BehandlingStatus status;

    @Column(name = "behandling_tidspunkt")
    private LocalDateTime behandlingTidspunkt;
}

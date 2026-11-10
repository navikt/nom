package no.nav.nom.apps.sfintegrasjon.sfhendelse.repository;

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
import no.nav.nom.apps.sfintegrasjon.sfhendelse.dto.HendelseDiff;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@Builder
@Entity
@Table(name = "sf_hendelse_behandling")
@NoArgsConstructor
@AllArgsConstructor
public class SfHendelseBehandling {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "navident")
    private String navident;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endring", columnDefinition = "jsonb", nullable = false)
    private List<HendelseDiff> endring;

    @Column(name = "behandlings_id")
    private Long behandlingsId;
}

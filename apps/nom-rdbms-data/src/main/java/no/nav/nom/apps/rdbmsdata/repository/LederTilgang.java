package no.nav.nom.apps.rdbmsdata.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.state.LederTilgangState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "leder_tilgang")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class LederTilgang {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "til")
    private String til;

    @Column(name = "av")
    private String av;

    @Column(name = "orgenhet_id")
    private String orgenhetId;

    @Column(name = "gyldig_fom")
    private LocalDate gyldigFom;

    @Column(name = "gyldig_tom")
    private LocalDate gyldigTom;

    public static LederTilgang from(LederTilgangState lederTilgangState) {
        return LederTilgang.builder()
            .fid(lederTilgangState.fid())
            .til(lederTilgangState.til())
            .av(lederTilgangState.av())
            .orgenhetId(lederTilgangState.orgenhetId())
            .gyldigFom(lederTilgangState.gyldigFom())
            .gyldigTom(lederTilgangState.gyldigTom())
            .build();
    }
}

package no.nav.nom.apps.rdbmsdata.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "orgenhet_leder")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
@Getter
@ToString
public class OrgenhetLeder {
    @Id
    @Column(name = "fid")
    private String fid;

    @Column(name = "navident")
    private String navident;

    @Column(name = "orgenhet_id")
    private String orgEnhetId;

    @Column(name = "start_dato")
    private LocalDate startdato;

    @Column(name = "slutt_dato")
    private LocalDate sluttdato;

    @Column(name = "sist_endret_av")
    private String sistEndretAv;

    public OrgenhetLeder(String fid) {
        this.fid = fid;
    }

    public static OrgenhetLeder from(OrgEnhetLederState orgEnhetLederState) {
        return OrgenhetLeder.builder()
            .fid(orgEnhetLederState.fid())
            .navident(orgEnhetLederState.navident())
            .orgEnhetId(orgEnhetLederState.orgEnhetId())
            .startdato(orgEnhetLederState.startdato())
            .sluttdato(orgEnhetLederState.sluttdato())
            .sistEndretAv(orgEnhetLederState.sistEndretAv())
            .build();
    }
}


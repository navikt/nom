package no.nav.nom.contract.nom.api.graphql;

import lombok.Value;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;

import java.time.LocalDate;

@Value
public class RessursLeder {

    Ressurs ressurs;
    boolean erDagligOppfolging;
    OrgEnhet orgEnhet;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public RessursLeder(Ressurs ressurs, boolean erDagligOppfolging, OrgEnhet orgEnhet, LocalDate gyldigFom, LocalDate gyldigTom) {
        this.ressurs = ressurs;
        this.erDagligOppfolging = erDagligOppfolging;
        this.orgEnhet = orgEnhet;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
    }
}

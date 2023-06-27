package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
//@Builder
//@AllArgsConstructor
public class RessursLeder {

    Ressurs ressurs;
    boolean erDagligOppfolging;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    public RessursLeder(Ressurs ressurs, boolean erDagligOppfolging) {
        this.ressurs = ressurs;
        this.erDagligOppfolging = erDagligOppfolging;
        this.gyldigFom = LocalDate.now();
        this.gyldigTom = null;
    }
}

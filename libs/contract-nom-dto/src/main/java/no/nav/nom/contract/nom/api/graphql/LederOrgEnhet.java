package no.nav.nom.contract.nom.api.graphql;

import lombok.Value;

import java.time.LocalDate;

@Value
public class LederOrgEnhet {
    OrgEnhet orgEnhet;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public LederOrgEnhet(OrgEnhet orgEnhet, LocalDate gyldigFom, LocalDate gyldigTom) {
        this.orgEnhet = orgEnhet;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
    }
}

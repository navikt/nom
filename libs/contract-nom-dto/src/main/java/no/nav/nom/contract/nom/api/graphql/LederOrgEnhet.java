package no.nav.nom.contract.nom.api.graphql;

import lombok.Value;

import java.time.LocalDate;

@Value
public class LederOrgEnhet {
    OrgEnhet organisasjonsenhet;
    OrgEnhet orgEnhet;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public LederOrgEnhet(OrgEnhet orgEnhet) {
        this.organisasjonsenhet = orgEnhet;
        this.orgEnhet = orgEnhet;
        this.gyldigFom = LocalDate.now();
        this.gyldigTom = null;
    }
}

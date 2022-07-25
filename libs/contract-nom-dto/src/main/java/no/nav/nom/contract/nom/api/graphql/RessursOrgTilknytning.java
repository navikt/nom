package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class RessursOrgTilknytning {
    OrgEnhet organisasjonsenhet;
    OrgEnhet orgEnhet;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
}

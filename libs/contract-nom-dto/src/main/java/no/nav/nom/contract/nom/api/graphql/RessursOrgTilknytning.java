package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;

@Value
@Builder
public class RessursOrgTilknytning {
    OrgEnhet orgEnhet;
    boolean erDagligOppfolging;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    Sektor sektor;
    Tilknytningtype tilknytningType;
}

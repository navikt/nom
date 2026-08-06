package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.List;

@Builder
@Value
public class OrgTilknytning {
    Ressurs ressurs;
    boolean erDagligOppfolging;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    Sektor sektor;
    Tilknytningtype tilknytningType;
    String primaryFysiskBrukersted;
    List<String> additionalFysiskeBrukerstederNummer;
}

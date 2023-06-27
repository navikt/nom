package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Builder
@Value
public class OrgEnhetsKobling {
    Ressurs ressurs;
    boolean erDagligOppfolging;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
}

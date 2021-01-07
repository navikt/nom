package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Builder
@Value
public class OrganisasjonsenhetsKobling {
    Ressurs ressurs;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
}

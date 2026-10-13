package no.nav.nom.apps.sfintegrasjon.orgenhet.domain;

import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.List;

@Builder(toBuilder = true)
public record SfOrgEnhet(
    String id,
    String navn,
    NomNivaa nivaa,
    boolean erLovnode,
    OrgEnhetsType type,
    String tilgangsenhet,
    String kostnadssted,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    List<Leder> ledere,
    List<OverOrgEnhet> overOrgEnheter
) {
}

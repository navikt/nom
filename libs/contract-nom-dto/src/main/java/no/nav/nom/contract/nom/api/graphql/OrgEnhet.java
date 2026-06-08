package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Value
@Builder
public class OrgEnhet {
    @Deprecated        String agressoId;
    @Deprecated        String orgNiv;
    @Deprecated        String agressoOrgenhetType;

    String id;
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    NomNivaa nomNivaa;

    OrgEnhetsType orgEnhetsType;
    String remedyEnhetId;
    String tilgangsenhetId;

    AgressoInfo agressoInfo;

    // Enheter som er opprettet og avsluttet samme dag er ikke gyldige
    public boolean endedOnStartDate() {
        return getGyldigTom() != null && getGyldigFom().isEqual(getGyldigTom());
    }
}

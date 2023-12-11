package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

@Value
@Builder
public class OrgEnhet {
    String id;
    String agressoId;
    String orgNiv;
    String agressoOrgenhetType;
    String navn;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    @Deprecated
    Kode type;

    @Deprecated
    NomNivaa orgEnhetsKategori;
    NomNivaa nomNivaa;

    OrgEnhetsType orgEnhetsType;
    String remedyEnhetId;

    // Enheter som er opprettet og avsluttet samme dag er ikke gyldige
    public boolean endedOnStartDate() {
        return getGyldigTom() != null && getGyldigFom().isEqual(getGyldigTom());
    }
}

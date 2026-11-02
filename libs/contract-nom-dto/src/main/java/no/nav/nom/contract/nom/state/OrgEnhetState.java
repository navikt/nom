package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;

import static java.util.Objects.nonNull;

@Builder(toBuilder = true)
public record OrgEnhetState (
    String fid,
    String id,  // Ikke kryptisk uid, ment for menneskelig daglig bruk
    String navn,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    String overordnetOrgEnhetFid,
    String lederFid,
    OrgEnhetsType orgEnhetsType,
    NomNivaa nomNivaa,
    String uniqueAgressoId,
    String agressoId,
    String agressoOrgNivaa,
    String agressoOrgenhetType,
    String tilgangsenhetId,
    String kostnadsstedId
) {
    // Bakoverkompatibel konstruktør uten kostnadssted
    public OrgEnhetState(
        String fid,
        String id,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        String overordnetOrgEnhetFid,
        String lederFid,
        OrgEnhetsType orgEnhetsType,
        NomNivaa nomNivaa,
        String uniqueAgressoId,
        String agressoId,
        String agressoOrgNivaa,
        String agressoOrgenhetType,
        String tilgangsenhetId
    ) {
        this(fid, id, navn, gyldigFom, gyldigTom, overordnetOrgEnhetFid, lederFid, orgEnhetsType, nomNivaa,
            uniqueAgressoId, agressoId, agressoOrgNivaa, agressoOrgenhetType, tilgangsenhetId, null);
    }

    // Enheter som er opprettet og avsluttet samme dag er ikke gyldige
    public boolean endedOnStartDate() {
        return nonNull(gyldigTom()) && gyldigFom().isEqual(gyldigTom());
    }

    @JsonIgnore
    public boolean isActive() {
        // når gyldigTom er null, orgenhetvurderes som aktiv
        if(nonNull(gyldigTom())) return !endedOnStartDate() && gyldigTom().isAfter(LocalDate.now().minusDays(1));
        else return true;
    }
}

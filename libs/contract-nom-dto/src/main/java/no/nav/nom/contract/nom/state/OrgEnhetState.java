package no.nav.nom.contract.nom.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.Optional;

@Builder(toBuilder = true)
public record OrgEnhetState (
    String fid,
    String id,  // Ikke kryptisk uid, ment for menneskelig daglig bruk
    String navn,
    LocalDate gyldigFom,
    Optional<LocalDate> gyldigTom,
    Optional<String> overordnetOrgEnhetFid,
    Optional<String> lederFid,
    Optional<OrgEnhetsType> orgEnhetsType,
    Optional<NomNivaa> nomNivaa,
    Optional<String> uniqueAgressoId,
    Optional<String> agressoId,
    Optional<String> agressoOrgNivaa,
    Optional<String> agressoOrgenhetType


) {
    // Enheter som er opprettet og avsluttet samme dag er ikke gyldige
    public boolean endedOnStartDate() {
        return gyldigTom().isPresent() && gyldigFom().isEqual(gyldigTom().get());
    }

    @JsonIgnore
    public boolean isActive() {
        // return !endedOnStartDate() && gyldigTom().isPresent() && gyldigTom().get().isAfter(LocalDate.now().minusDays(1));
        // når gyldigTom er null, orgenhetvurderes som aktiv
        if(gyldigTom().isPresent()) return !endedOnStartDate() && gyldigTom().get().isAfter(LocalDate.now().minusDays(1));
        else return true;
    }
}

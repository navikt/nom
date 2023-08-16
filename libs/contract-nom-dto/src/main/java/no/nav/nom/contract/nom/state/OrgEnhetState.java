package no.nav.nom.contract.nom.state;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.Optional;

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
) {}

package no.nav.nom.contract.nom.state;

import no.nav.nom.contract.nom.OrgEnhetsKategori;

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
    Optional<OrgEnhetsKategori> orgEnhetsKategori
) {}

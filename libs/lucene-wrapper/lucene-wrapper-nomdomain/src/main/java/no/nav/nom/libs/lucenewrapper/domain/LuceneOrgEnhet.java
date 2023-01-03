package no.nav.nom.libs.lucenewrapper.domain;

import java.time.LocalDate;
import java.util.Optional;

public record LuceneOrgEnhet(String id, String navn, String enhetType,String orgNiv, Optional<LocalDate> gyldigTom) {
    public LuceneOrgEnhet(String id, String navn, String enhetType) {
        this(id,navn,enhetType,"ORGENHET", Optional.of(LocalDate.of(2000, 1, 1)));
    }
}

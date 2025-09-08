package no.nav.nom.libs.lucenewrapper.domain;

import java.time.LocalDate;

public record LuceneOrgEnhet(String id, String navn, String enhetType,String orgNiv, LocalDate gyldigTom) {
    public LuceneOrgEnhet(String id, String navn, String enhetType) {
        this(id,navn,enhetType,"ORGENHET", LocalDate.of(2000, 1, 1));
    }
}

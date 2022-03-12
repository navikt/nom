package no.nav.nom.apps.api.kobling.contract;

import java.time.LocalDate;
import java.util.Optional;

public interface Kobling {
    String getAgressoId();
    String getOrgNiv();
    String getOrgenhetNavn();
    String getNavIdent();
    LocalDate getGyldigFom();
    Optional<LocalDate> getGyldigTom();
}

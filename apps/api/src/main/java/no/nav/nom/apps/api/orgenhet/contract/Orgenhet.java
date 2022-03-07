package no.nav.nom.apps.api.orgenhet.contract;

import java.time.LocalDate;
import java.util.Optional;

public interface Orgenhet {
    String getAgressoId();
    String getNavn();
    String getOrgNiv();
    LocalDate getGyldigFom();
    Optional<LocalDate> getGyldigTom();
}

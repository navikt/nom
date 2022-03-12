package no.nav.nom.apps.api.orgenhet.contract;

import java.time.LocalDate;
import java.util.Optional;

public interface IOrgenhet {
    String getAgressoId();
    String getNavn();
    String getOrgNiv();
    OrgenhetType getOrgenhetType();
    LocalDate getGyldigFom();
    Optional<LocalDate> getGyldigTom();
}

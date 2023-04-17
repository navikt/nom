package no.nav.nom.apps.api.orgtilknytning.contract;

import java.time.LocalDate;
import java.util.Optional;

public interface Orgtilknytning {
    String getAgressoId();

    String getOrgNiv();

    String getOrgenhetNavn();

    String getOrgEnhetId();

    String getNavIdent();

    LocalDate getGyldigFom();

    Optional<LocalDate> getGyldigTom();
}

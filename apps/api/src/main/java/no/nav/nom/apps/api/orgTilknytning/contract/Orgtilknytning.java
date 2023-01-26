package no.nav.nom.apps.api.orgTilknytning.contract;

import java.time.LocalDate;
import java.util.Optional;

public interface Orgtilknytning {
    String getAgressoId();

    String getOrgNiv();

    String getOrgenhetNavn();

    String getNavIdent();

    LocalDate getGyldigFom();

    Optional<LocalDate> getGyldigTom();
}

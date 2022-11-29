package no.nav.nom.apps.api.orgenhet.contract;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IOrgenhet {
    UUID getFid();

    String getAgressoId();

    String getAgressoOrgenhetType();

    String getNavn();

    String getOrgNiv();

    LocalDate getGyldigFom();

    Optional<LocalDate> getGyldigTom();

    Optional<OrgEnhetsKategori> getOrgEnhetsKategori();

    String getUniqueAgressoId();
}

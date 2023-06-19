package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.Optional;

public interface IOrgenhet {
    String getFid();

    String getId();

    String getAgressoId();

    String getAgressoOrgenhetType();

    String getNavn();

    String getOrgNiv();

    LocalDate getGyldigFom();

    Optional<LocalDate> getGyldigTom();

    Optional<NomNivaa> getNomNivaa();

    Optional<OrgEnhetsType> getOrgEnhetsType();

    String getUniqueAgressoId();

    Optional<String> getOverordnetOrgEnhetFid();

    record AgressoIdCombo(
            String agressoId,
            String orgNiv
    ){
        @Override
        public String toString() {
            return orgNiv + "--" + agressoId;
        }
    }
}

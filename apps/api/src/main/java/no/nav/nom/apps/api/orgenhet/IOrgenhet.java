package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IOrgenhet {
    String getFid();

    String getId();

    String getAgressoId();

    String getAgressoOrgenhetType();

    String getNavn();

    String getOrgNiv();

    LocalDate getGyldigFom();

    Optional<LocalDate> getGyldigTom();

    Optional<OrgEnhetsKategori> getOrgEnhetsKategori();

    String getUniqueAgressoId();

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

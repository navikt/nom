package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;

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

    // Enheter som er opprettet og avsluttet samme dag er ikke gyldige
    default boolean endedOnStartDate() {
        return getGyldigTom().isPresent() && getGyldigFom().isEqual(getGyldigTom().get());
    }

    default boolean erORGENHETnode() { return "ORGENHET".equals(getOrgNiv()); }

    default boolean overlapper(LocalDate FOMDato, LocalDate TOMDato) {
        return likEllerFoer(getGyldigFom(), TOMDato)
            && likEllerEtter(getGyldigTom().orElse(NO_END_DATE), FOMDato);
    }
}

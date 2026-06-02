package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.time.LocalDate;
import java.util.Objects;

import static java.util.Objects.nonNull;
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

    LocalDate getGyldigTom();

    NomNivaa getNomNivaa();

    OrgEnhetsType getOrgEnhetsType();

    String getUniqueAgressoId();

    String getOverordnetOrgEnhetFid();

    String getTilgangsenhetId();

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
        return nonNull(getGyldigTom()) && getGyldigFom().isEqual(getGyldigTom());
    }

    default boolean erORGENHETnode() { return "ORGENHET".equals(getOrgNiv()); }

    default boolean overlapper(LocalDate FOMDato, LocalDate TOMDato) {
        return likEllerFoer(getGyldigFom(), TOMDato)
            && likEllerEtter(Objects.requireNonNullElse(getGyldigTom(), NO_END_DATE), FOMDato);
    }

    default boolean erAktivForDato(LocalDate dato) {
        return likEllerFoer(getGyldigFom(), dato)
            && likEllerEtter(Objects.requireNonNullElse(getGyldigTom(), NO_END_DATE), dato);
    }
}

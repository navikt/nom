package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record StillingPeriode(
    StillingAgressoOrgIdOgNavn stilling,
    LocalDate gyldigFom,
    LocalDate gyldigTom,  // Aldri null. Open ended er 2099-12-31
    IOrgenhet orgenhet,
    DagligOppfolging dagligOppfolging
) {
    public StillingPeriode(StillingPeriode copy, IOrgenhet orgenhet) {
        this(copy.stilling, copy.gyldigFom, copy.gyldigTom, orgenhet, copy.dagligOppfolging);
    }

    public StillingPeriode(StillingPeriode copy, DagligOppfolging erDagligOppfolging) {
        this(copy.stilling, copy.gyldigFom, copy.gyldigTom, copy.orgenhet, erDagligOppfolging);
    }

    public StillingPeriode(StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn, LocalDate gyldigFom, LocalDate gyldigTom, IOrgenhet orgenhet) {
        this(stillingAgressoOrgIdOgNavn, gyldigFom, gyldigTom, orgenhet, DagligOppfolging.UKJENT);
    }

    @Override
    public String toString() {
        return (stilling == null ? "Ingen stilling" : stilling.toString()) + ": " + gyldigFom + " - " + gyldigTom + " for nom orgenhet [" + (orgenhet != null ? orgenhet.getId() + "-" + orgenhet.getNavn() : " - ") + "] er dagligOppfølging: " + dagligOppfolging;
    }

    public static String toString(List<StillingPeriode> stillingPerioder) {
        return StillingPeriode.toString(stillingPerioder, false);
    }

    public static String toString(List<StillingPeriode> stillingPerioder, boolean indent) {
        if (stillingPerioder == null || stillingPerioder.isEmpty()) {
            return "Ingen stillingPerioder";
        }
        StringBuffer str = new StringBuffer();
        stillingPerioder.forEach( s -> str.append(indent ? "  " : "").append(s).append("\n"));
        return str.toString();
    }

    public boolean harSammeAgressoIdPaStillingOgRessurs() {
        return this.stilling.agressoId.isPresent()
            && this.stilling.ressursAgressoId.isPresent()
            && this.stilling.agressoId.get().equals(this.stilling.ressursAgressoId.get());
    }

    public record StillingAgressoOrgIdOgNavn(
        Optional<String> agressoId,
        String enhetNavn,
        Optional<String> ressursAgressoId,
        Optional<String> ressursAgressoOrgNiv
    ) {
        @Override
        public String toString() {
            return "HRRES_STILLING[" + agressoId.orElse("NULL") + "-" + enhetNavn + "]/HRRES_RESSURS[" + ressursAgressoId.orElse("NULL") + "-" + ressursAgressoOrgNiv.orElse(
                "NULL") + "]";
        }
    }

    enum DagligOppfolging {
        JA, NEI, UKJENT
    }
}

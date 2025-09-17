package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

public record StillingPeriode(
    StillingAgressoOrgIdOgNavn stilling,
    LocalDate gyldigFom,
    LocalDate gyldigTom,  // Aldri null. Open ended er 2099-12-31
    IOrgenhet orgenhet,
    DagligOppfolging dagligOppfolging,
    Sektor sektor,
    Tilknytningtype tilknytningtype
) {
    public StillingPeriode(StillingPeriode copy, IOrgenhet orgenhet) {
        this(copy.stilling, copy.gyldigFom, copy.gyldigTom, orgenhet, copy.dagligOppfolging, copy.sektor, copy.tilknytningtype);
    }

    public StillingPeriode(StillingPeriode copy, DagligOppfolging erDagligOppfolging) {
        this(copy.stilling, copy.gyldigFom, copy.gyldigTom, copy.orgenhet, erDagligOppfolging, copy.sektor, copy.tilknytningtype);
    }

    public StillingPeriode(StillingPeriode copy, LocalDate gyldigFom, LocalDate gyldigTom) {
        this(copy.stilling, gyldigFom, gyldigTom, copy.orgenhet, copy.dagligOppfolging, copy.sektor, copy.tilknytningtype);
    }

    public StillingPeriode(StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn, LocalDate gyldigFom, LocalDate gyldigTom, IOrgenhet orgenhet) {
        this(stillingAgressoOrgIdOgNavn, gyldigFom, gyldigTom, orgenhet, DagligOppfolging.UKJENT, null, null);
    }

    public StillingPeriode(StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn, LocalDate gyldigFom, LocalDate gyldigTom, IOrgenhet orgenhet, Sektor sektor, Tilknytningtype tilknytningtype) {
        this(stillingAgressoOrgIdOgNavn, gyldigFom, gyldigTom, orgenhet, DagligOppfolging.UKJENT, sektor, tilknytningtype);
    }

    public StillingPeriode(StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn, LocalDate gyldigFom, LocalDate gyldigTom, IOrgenhet orgenhet, DagligOppfolging dagligOppfolging) {
        this(stillingAgressoOrgIdOgNavn, gyldigFom, gyldigTom, orgenhet, dagligOppfolging, null, null);
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
        return nonNull(this.stilling.agressoId)
            && nonNull(this.stilling.ressursAgressoId)
            && this.stilling.agressoId.equals(this.stilling.ressursAgressoId);
    }

    public record StillingAgressoOrgIdOgNavn(
        String agressoId,
        String enhetNavn,
        String ressursAgressoId,
        String ressursAgressoOrgNiv
    ) {
        @Override
        public String toString() {
            return "HRRES_STILLING[" + agressoId + "-" + enhetNavn + "]/HRRES_RESSURS[" + ressursAgressoId + "-" + ressursAgressoOrgNiv + "]";
        }
    }

    public boolean erAgressoIdPresis() {
        return nonNull(stilling.agressoId) && stilling.agressoId.length() > 6;
    }

    public boolean erRessursAgressoIdPresis() {
        return nonNull(stilling.ressursAgressoId) && stilling.ressursAgressoId.length() > 6;
    }

    public boolean manglerAgressoId() {
        return isNull(stilling.agressoId) || stilling.agressoId.isBlank() || stilling.agressoId.startsWith("-");
    }

    public boolean manglerRessursAgressoId() {
        return isNull(stilling.ressursAgressoId) || stilling.ressursAgressoId.isBlank() || stilling.ressursAgressoId.startsWith("-");
    }

    public boolean matcherRessursAgressoIdOgAgressoId() {
        if (erAgressoIdPresis()) {
            return stilling.agressoId.startsWith(requireNonNullElse(stilling.ressursAgressoId, "-"));
        } else if (erRessursAgressoIdPresis()) {
            return stilling.ressursAgressoId.startsWith(requireNonNullElse(stilling.agressoId, "-"));
        }
        return false;
    }

    public String getPresisAgressoId() {
        if (erAgressoIdPresis()) {
            return stilling.agressoId;
        }
        if (erRessursAgressoIdPresis() && matcherRessursAgressoIdOgAgressoId()) {
            return stilling.ressursAgressoId;
        }
        if (erRessursAgressoIdPresis() && isCurrentPeriod()) {
            return stilling.ressursAgressoId;
        }
        return null;
    }

    public boolean isCurrentPeriod() {
        return PeriodeUtil.isCurrentPeriod(this);
    }

    public boolean covers(LocalDate date) {
        return PeriodeUtil.intervalCoversDate(this, date);
    }

    public boolean overlaps(OrgtilknytningState other) {
        return PeriodeUtil.overlapsInterval(this, other);
    }

    public enum DagligOppfolging {
        JA, NEI, UKJENT
    }

    public boolean erDagligOppfolging() {
        return dagligOppfolging == DagligOppfolging.JA;
    }
}

package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    public boolean erAgressoIdPresis() {
        return stilling.agressoId.isPresent() && stilling.agressoId.get().length() > 6;
    }

    public boolean erRessursAgressoIdPresis() {
        return stilling.ressursAgressoId.isPresent() && stilling.ressursAgressoId.get().length() > 6;
    }

    public boolean manglerAgressoId() {
        return stilling.agressoId.isEmpty() || stilling.agressoId.get().isBlank() || stilling.agressoId.get().startsWith("-");
    }

    public boolean manglerRessursAgressoId() {
        return stilling.ressursAgressoId.isEmpty() || stilling.ressursAgressoId.get().isBlank() || stilling.ressursAgressoId.get().startsWith("-");
    }

    public boolean erRessursAgressoOrgNivORGENHETnode() {
        return "ORGENHET".equalsIgnoreCase(stilling.ressursAgressoOrgNiv.orElse(""));
    }

    public boolean erAgressoIdOgRessursAgressoIdLike() {
        return stilling.agressoId.equals(stilling.ressursAgressoId);
    }

    public boolean matcherRessursAgressoIdOgAgressoId() {
        if (erAgressoIdPresis()) {
            return stilling.agressoId.get().startsWith(stilling.ressursAgressoId.orElse("-"));
        } else if (erRessursAgressoIdPresis()) {
            return stilling.ressursAgressoId.get().startsWith(stilling.agressoId.orElse("-"));
        }
        return false;
    }

    public String getPresisAgressoId() {
        if (erAgressoIdPresis()) {
            return stilling.agressoId.get();
        }
        if (erRessursAgressoIdPresis() && matcherRessursAgressoIdOgAgressoId()) {
            return stilling.ressursAgressoId.get();
        }
        if (erRessursAgressoIdPresis() && isCurrentPeriod()) {
            return stilling.ressursAgressoId.get();
        }
        return null;
    }

    public boolean isCurrentPeriod() {
        return PeriodeUtil.isCurrentPeriod(this);
    }

    public boolean isHistoricPeriod() {
        return PeriodeUtil.isHistoricPeriod(this);
    }

    public boolean isFuturePeriod() {
        return PeriodeUtil.isFuturePeriod(this);
    }

    public boolean covers(LocalDate date) {
        return PeriodeUtil.intervalCoversDate(this, date);
    }

    public boolean overlaps(StillingPeriode other) {
        return PeriodeUtil.overlapsInterval(this, other);
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

package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

record StillingPeriode(
    StillingAgressoOrgIdOgNavn stilling,
    LocalDate gyldigFom,
    LocalDate gyldigTom,  // Aldri null. Open ended er 2099-12-31
    IOrgenhet orgenhet
) {
    public StillingPeriode(StillingPeriode copy, IOrgenhet orgenhet) {
        this(copy.stilling, copy.gyldigFom, copy.gyldigTom, orgenhet);
    }

    @Override
    public String toString() {
        return (stilling == null ? "Ingen stilling" : stilling.toString()) + ": " + gyldigFom + " - " + gyldigTom + " for nom orgenhet [" + (orgenhet != null ? orgenhet.getId() + "-" + orgenhet.getNavn() : " - ") + "]";
    }

    public static String stillingperioderToString(List<StillingPeriode> stillingPerioder) {
        return StillingPeriode.stillingperioderToString(stillingPerioder, false);
    }

    public static String stillingperioderToString(List<StillingPeriode> stillingPerioder, boolean indent) {
        StringBuffer str = new StringBuffer();
        stillingPerioder.forEach( s -> str.append(indent ? "  " : "").append(s).append("\n"));
        return str.toString();
    }

    record StillingAgressoOrgIdOgNavn(
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
}

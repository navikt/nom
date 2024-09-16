package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record StrukturerteStillingPerioderForNavident(
    String navident,
    List<StillingPeriode> stillingPerioder,
    List<DvhRessursStilling> dvhRessursStillinger,
    List<Error> errors
) {
    public StrukturerteStillingPerioderForNavident(String navident, List<StillingPeriode> stillingPerioder, List<DvhRessursStilling> ressursStillinger) {
        this(navident, stillingPerioder, ressursStillinger, new ArrayList<>());
    }

    public StrukturerteStillingPerioderForNavident(String navident, List<DvhRessursStilling> ressursStillinger) {
        this(navident, new ArrayList<>(), ressursStillinger, new ArrayList<>());
    }

    public StrukturerteStillingPerioderForNavident(StrukturerteStillingPerioderForNavident copy, List<StillingPeriode> stillingPerioder) {
        this(copy.navident, stillingPerioder, copy.dvhRessursStillinger, copy.errors);
    }

    public record  Error(
        ErrorType errorType,
        String message,
        DvhRessursStilling gjeldendeDvhResursStilling,
        StillingPeriode gjeldendeStillingPeriode
    ) {}

    public enum ErrorType
    {
        NAVIDENT_FINNES_IKKE,
        DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
        DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER,
        STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT,
        STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE,
        DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT,
        DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE,
        FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE,
        FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MEN_PERIDOE_ER_DEKT,
        FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT,
        FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_OG_HAR_INGEN_ANDRE_STILLINGSPERIODER_ELLER_ORGTILKNYTNINGER,
        AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER,
        KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET,
        HAR_LAGET_NEGATIV_STILLINGPERIODE__DENNE_FJERNES,
        HISTORIC_PERIODE_KAN_IKKE_SETTE_DOL,
        FUTURE_PERIODE_KAN_IKKE_SETTE_DOL,
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_I_HISTORISK_PERIODE,
        DUPLIKATE_EKSISTERENDE_ORGTILKNYNINGER_MED_DOL_I_HISTORISK_PERIODE,
        CURRENT_PERIODE_KAN_IKKE_MATCHE_ORGENHET_PAA_RESSURS_FOR_DOL
    }

    public void addError(ErrorType errorType, String message, StillingPeriode gjeldendeStillingPeriode) {
        errors().add(new Error(errorType, message, null, gjeldendeStillingPeriode));
    }

    public void addError(ErrorType errorType, String message, DvhRessursStilling gjeldendeDvhResursStilling) {
        errors().add(new Error(errorType, message, gjeldendeDvhResursStilling, null));
    }

    public void addError(ErrorType errorType, String message) {
        errors().add(new Error(errorType, message, null, null));
    }

    public void addStillingPerioder(Collection<StillingPeriode> stillingPeriode) {
        stillingPerioder().addAll(stillingPeriode);
    }

    public long countOfErrorType(ErrorType errorType) {
        return errors().stream().filter(e -> e.errorType().equals(errorType)).count();
    }
}

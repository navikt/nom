package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OrgtilknytningCommand;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record StrukturerteStillingPerioderForNavident(
    String navident,
    List<StillingPeriode> stillingPerioder,
    List<DvhRessursStilling> dvhRessursStillinger,
    List<OrgtilknytningState> eksisterendeOrgtilknytninger,
    List<Endring> endringer,
    List<Error> errors,
    String endringsLogg
) {
    public StrukturerteStillingPerioderForNavident(String navident, List<StillingPeriode> stillingPerioder, List<DvhRessursStilling> ressursStillinger) {
        this(navident, stillingPerioder, ressursStillinger, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
    }

    public StrukturerteStillingPerioderForNavident(String navident, List<DvhRessursStilling> ressursStillinger) {
        this(navident, new ArrayList<>(), ressursStillinger, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null);
    }

    public StrukturerteStillingPerioderForNavident(StrukturerteStillingPerioderForNavident copy, List<StillingPeriode> stillingPerioder) {
        this(copy.navident, stillingPerioder, copy.dvhRessursStillinger, new ArrayList<>(), new ArrayList<>(), copy.errors, null);
    }

    public StrukturerteStillingPerioderForNavident(StrukturerteStillingPerioderForNavident copy, List<OrgtilknytningState> eksisterendeOrgtilknytninger, List<AbstractMap.SimpleEntry<String, OrgtilknytningCommand>> endringer) {
        this(copy.navident, copy.stillingPerioder, copy.dvhRessursStillinger, eksisterendeOrgtilknytninger, from(endringer), copy.errors, null);
    }

    public StrukturerteStillingPerioderForNavident(String navident, List<StillingPeriode> stillingPerioder, List<AbstractMap.SimpleEntry<String, OrgtilknytningCommand>> simpleEntries, String endringsLogg) {
        this(navident, stillingPerioder, new ArrayList<>(), new ArrayList<>(), from(simpleEntries), new ArrayList<>(), endringsLogg);
    }

    public record Endring(
        String navident,
        String type,
        OrgtilknytningCommand command
    ) {}

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
        STILLINGPERIODE_ER_UTENFOR_RESSURSENS_PERIODE,
        STILLINGPERIODE_ER_UTENFOR_ORGENHET_PERIODE,
        STILLINGPERIODER_HAR_OVERLAPPENDE_DOL_PERIODER,
        STILLINGPERIODE_HAR_NEGATIV_PERIODE,
        AKTIV_DOL_MATCHER_IKKE_ORGENHET_PAA_RESSURS,
        HULL_I_STILLINGSPERIODE,
        MULTI_MATCH_ORGENHET_PAA_RESSURS,
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

    private static List<Endring> from(List<AbstractMap.SimpleEntry<String, OrgtilknytningCommand>> endringer) {
        List<Endring> result = new ArrayList<>();
        if (endringer == null) {
            return result;
        }
        endringer.forEach( e -> {
            result.add(new Endring(e.getKey(), e.getValue().getClass().getSimpleName(), e.getValue()));
        });
        return result;
    }


    public List<String> diff(StrukturerteStillingPerioderForNavident gmlSSPFN) {
        List<String> differanser = new ArrayList<>();
        if (stillingPerioder.size() != gmlSSPFN.stillingPerioder.size()) {
            differanser.add("Antall stillingPerioder er ulik (Nye/gml): " + stillingPerioder.size() + " / " + gmlSSPFN.stillingPerioder.size());
        }
        if (endringer.size() != gmlSSPFN.endringer.size()) {
            differanser.add("Antall endringer er ulik (Nye/gml): " + endringer.size() + " / " + gmlSSPFN.endringer.size());
            differanser.add("Nye endringer: " + endringer);
            differanser.add("Gml endringer: " + gmlSSPFN.endringer);
        }
        return differanser;
    }
}

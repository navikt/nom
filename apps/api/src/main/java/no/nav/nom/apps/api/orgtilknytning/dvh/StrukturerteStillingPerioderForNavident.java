package no.nav.nom.apps.api.orgtilknytning.dvh;

import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record StrukturerteStillingPerioderForNavident(
    String navident,
    List<StillingPeriode> stillingPerioder,
    List<DvhRessursStilling> dvhRessursStillinger,
    List<Error> errors
) {
    public StrukturerteStillingPerioderForNavident(String navident, ArrayList<StillingPeriode> stillingPerioder, List<DvhRessursStilling> ressursStillinger) {
        this(navident, stillingPerioder, ressursStillinger, new ArrayList<>());
    }

    public StrukturerteStillingPerioderForNavident(String navident, List<DvhRessursStilling> ressursStillinger) {
        this(navident, new ArrayList<>(), ressursStillinger, new ArrayList<>());
    }

    public record  Error(
        ErrorType errorType,
        String message,
        DvhRessursStilling gjeldendeDvhResursStilling
    ) {}

    public enum ErrorType
    {
        NAVIDENT_FINNES_IKKE,
        DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
        DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER
    }

    public void addError(ErrorType errorType, String message, DvhRessursStilling gjeldendeDvhResursStilling) {
        errors().add(new Error(errorType, message, gjeldendeDvhResursStilling));
    }

    public void addError(ErrorType errorType, String message) {
        errors().add(new Error(errorType, message, null));
    }

    public void addStillingPeriode(StillingPeriode stillingPeriode) {
        stillingPerioder().add(stillingPeriode);
    }

    public void addStillingPerioder(Collection<StillingPeriode> stillingPeriode) {
        stillingPerioder().addAll(stillingPeriode);
    }

}

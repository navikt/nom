package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

import no.nav.nom.libs.utils.common.util.DateUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class InvalidOppdaterOrgtilknytningerForRessursRequestException extends RuntimeException {

    public InvalidOppdaterOrgtilknytningerForRessursRequestException(String message) {
        super(message);
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException duplicateOrgtilknytningFids(Collection<String> duplicateFids) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Kan ikke oppdatere flere elementer med samme fid (%s)".formatted(duplicateFids)
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException missingOrgtilknytningFids(Collection<String> duplicateFids) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Mangler øyeblikksbilde av orgtilknytninger med fid %s".formatted(duplicateFids)
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException orgtilknytningerNotInRessurs(Collection<String> fidsNotInRessurs) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Ressurs har ingen orgtilknytninger med fid %s".formatted(fidsNotInRessurs)
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException gyldigFomAfterGyldigTom(LocalDate gyldigFom, LocalDate gyldigTom) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "'gyldigFom' (%s) kan ikke være en senere dato enn 'gyldigTom' (%s)".formatted(gyldigFom, gyldigTom)
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException overlappendeDagligeOppfolginger(List<DateUtils.Periode> overlappendePerioder) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Request inneholder overlappende perioder for daglig oppfølging i periodene %s".formatted(
                overlappendePerioder.stream().map(
                    periode -> "%s - %s".formatted(periode.start(), periode.end())
                ).toList()
            )
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException duplikatOrgtilknytning(String orgenhetNavn, String orgenhetId) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Duplikat orgtilknytning for orgenhet %s (%s)".formatted(orgenhetNavn, orgenhetId)
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException vikafossenIdentPaaFeilEnhet() {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Ressursen er av type VIKAFOSSEN og kan da kun ha orgtilknytning til Vikafossen enheten"
        );
    }

    public static InvalidOppdaterOrgtilknytningerForRessursRequestException ikkeVikafossenIdentPaaVikafossenEnhet(String ressurstype) {
        return new InvalidOppdaterOrgtilknytningerForRessursRequestException(
            "Ressursen er av type {} og kan da ikke ha orgtilknytning til Vikafossen enheten"
        );
    }

}

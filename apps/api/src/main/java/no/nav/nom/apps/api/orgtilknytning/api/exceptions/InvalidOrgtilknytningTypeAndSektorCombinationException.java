package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

import no.nav.nom.contract.nom.OrgtilknytningType;
import no.nav.nom.contract.nom.Sektor;

public class InvalidOrgtilknytningTypeAndSektorCombinationException extends RuntimeException {

    public InvalidOrgtilknytningTypeAndSektorCombinationException(String message) {
        super(message);
    }

    public static InvalidOrgtilknytningTypeAndSektorCombinationException forCombination(
        OrgtilknytningType orgtilknytningType, Sektor sektor
    ) {
        return new InvalidOrgtilknytningTypeAndSektorCombinationException(
            "orgtilknytningType %s not allowed for ressurs with sektor %s"
                .formatted(orgtilknytningType, sektor)
        );
    }
}

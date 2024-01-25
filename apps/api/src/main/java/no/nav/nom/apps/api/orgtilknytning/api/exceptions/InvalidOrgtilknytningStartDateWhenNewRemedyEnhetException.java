package no.nav.nom.apps.api.orgtilknytning.api.exceptions;

// https://jira.adeo.no/browse/ORG-1759
public class InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException extends RuntimeException {

    public InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException(String message) {
        super(message);
    }
}

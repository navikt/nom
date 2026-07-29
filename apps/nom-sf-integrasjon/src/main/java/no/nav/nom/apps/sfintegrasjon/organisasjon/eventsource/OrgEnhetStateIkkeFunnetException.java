package no.nav.nom.apps.sfintegrasjon.organisasjon.eventsource;

/**
 * Kastes når OrgEnhet state ikke kan finnes for en FID etter retry.
 * Fører til at orgenhet evtsrc listener stoppes inntil problemet er løst manuelt.
 */
public class OrgEnhetStateIkkeFunnetException extends RuntimeException {
    public OrgEnhetStateIkkeFunnetException(String message) {
        super(message);
    }
}

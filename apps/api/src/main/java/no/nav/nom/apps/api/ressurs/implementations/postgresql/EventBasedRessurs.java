package no.nav.nom.apps.api.ressurs.implementations.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.repository.hendelse.Hendelse;
import no.nav.nom.apps.api.ressurs.contract.RessursI;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Comparator.comparing;

public class EventBasedRessurs implements RessursI {

    public final static String NAVIDENT_FIELD = "navIdent";
    public final static String PERSONIDENT_FIELD = "personIdent";

    private final List<Hendelse> hendelser;

    public EventBasedRessurs(List<Hendelse> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(Hendelse::getOpprettetTidspunkt)).toList();
    }

    @Override
    public UUID getId() {
        return hendelser.stream()
                .findFirst()
                .map(Hendelse::getFunksjonellId)
                .orElseThrow();
    }

    @Override
    public String getPersonIdent() {
        return getLast(PERSONIDENT_FIELD).orElseThrow();
    }

    @Override
    public String getNavIdent() {
        return getLast(NAVIDENT_FIELD).orElseThrow();
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .map(e -> e.getData().get(property))
                .filter(Objects::nonNull)
                .map(JsonNode::asText)
                .reduce((a, b) -> b);
    }
}

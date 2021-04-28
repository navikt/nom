package no.nav.nom.apps.api.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.val;
import no.nav.nom.contract.nom.hendelse.HendelseTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toUnmodifiableList;

@Value
public class Ressurs {

    List<HendelseTo> hendelser;

    public Ressurs(List<HendelseTo> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(HendelseTo::getOpprettet))
                .collect(toUnmodifiableList());
    }

    public UUID getId() {
        return hendelser.stream()
                .findFirst()
                .map(e -> e.getData().get("fid").asText())
                .map(UUID::fromString)
                .orElseThrow();
    }

    public String getPersonIdent() {
        return getLast("personIdent").orElseThrow();
    }

    public String getNavIdent() {
        return getLast("navIdent").orElseThrow();
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .map(e -> e.getData().get(property))
                .filter(Objects::nonNull)
                .map(JsonNode::asText)
                .reduce((a, b) -> b);
    }

    public Ressurs append(List<HendelseTo> appended) {
        val newHendelser = new ArrayList<HendelseTo>(hendelser.size() + appended.size());
        newHendelser.addAll(hendelser);
        newHendelser.addAll(appended);
        return new Ressurs(newHendelser);
    }
}

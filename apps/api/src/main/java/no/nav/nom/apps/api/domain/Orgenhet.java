package no.nav.nom.apps.api.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.val;
import no.nav.nom.contract.nom.hendelse.HendelseTo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Comparator.comparing;

/**
 * Trenger man egentlig periodiserte enheter?
 * Altså:
 * |------>|------> ?
 * Kan man ikke da si at det er en ny enhet? Hvorfor har man valgt å videreføre enheten per i dag?
 * Er det pga man ikke ville flytte ansatte? I NOM, kan det være en ny enhet men med f.eks samme AgressoNr?
 * ID: X    |--123--|
 * ID: Y             |---123-->
 */
@Value
public class Orgenhet {

    List<HendelseTo> hendelser;

    public Orgenhet(List<HendelseTo> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(HendelseTo::getOpprettet)).toList();
    }

    public UUID getId() {
        return hendelser.stream()
                .findFirst()
                .map(e -> e.getData().get("fid").asText())
                .map(UUID::fromString)
                .orElseThrow();
    }

    public String getAgressoId() {
        return getLast("agressoId").orElseThrow();
    }

    public String getNavn() {
        return getLast("navn").orElseThrow();
    }

    public LocalDate getGyldigFom() {
        return LocalDate.parse(getLast("gyldigFom").orElseThrow());
    }

    public Optional<LocalDate> getGyldigTom() {
        return getLast("gyldigTom").map(LocalDate::parse);
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .map(e -> e.getData().get(property))
                .filter(Objects::nonNull)
                .map(JsonNode::asText)
                .reduce((a, b) -> b);
    }

    public Orgenhet append(List<HendelseTo> appended) {
        val newHendelser = new ArrayList<HendelseTo>(hendelser.size() + appended.size());
        newHendelser.addAll(hendelser);
        newHendelser.addAll(appended);
        return new Orgenhet(newHendelser);
    }
}

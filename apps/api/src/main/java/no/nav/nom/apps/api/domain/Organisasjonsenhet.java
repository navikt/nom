package no.nav.nom.apps.api.domain;

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
import static java.util.stream.Collectors.toUnmodifiableList;

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
public class Organisasjonsenhet {
    List<HendelseTo> hendelser;

    public Organisasjonsenhet(List<HendelseTo> hendelser) {
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

    public String getNavn() {
        return hendelser.stream()
                .map(e -> e.getData().get("navn").asText())
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElseThrow(); // Regner med påkrevd, hvis ikke så får man ta Optional<String>
    }

    public LocalDate getGyldigFom() {
        return hendelser.stream()
                .map(e -> e.getData().get("gyldigFom").asText())
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .map(LocalDate::parse)
                .orElseThrow(); // Regner med påkrevd, hvis ikke så får man ta Optional<String>
    }

    public Optional<LocalDate> getGyldigTom() {
        return hendelser.stream()
                // Kun ta med de som har spesifisert et json element med gyldigTom.
                // Dette er for å støtte at man kan ha: Tom=2021.01.01   Tom=Null
                .filter(e -> e.getData().has("gyldigTom"))
                .map(e -> e.getData().get("gyldigTom").asText())
                .reduce((a, b) -> b)
                .map(LocalDate::parse);
    }

    public Organisasjonsenhet append(List<HendelseTo> appended) {
        val newHendelser = new ArrayList<HendelseTo>(hendelser.size() + appended.size());
        newHendelser.addAll(hendelser);
        newHendelser.addAll(appended);
        return new Organisasjonsenhet(newHendelser);
    }
}

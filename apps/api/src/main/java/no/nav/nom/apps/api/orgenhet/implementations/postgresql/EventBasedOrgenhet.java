package no.nav.nom.apps.api.orgenhet.implementations.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetType;

import java.time.LocalDate;
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
public class EventBasedOrgenhet implements IOrgenhet {

    public final static String AGRESSO_ID_FIELD = "agressoId";
    public final static String NAVN_FIELD = "navn";

    List<IHendelse> hendelser;

    public EventBasedOrgenhet(List<IHendelse> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(h -> h.getHendelseMetadata().getOpprettetTidspunkt())).toList();
    }

    public UUID getId() {
        return hendelser.stream()
                .findFirst()
                .map(IHendelse::getFid)
                .orElseThrow();
    }

    @Override
    public String getAgressoId() {
        return getLast("agressoId").orElseThrow();
    }

    @Override
    public String getNavn() {
        return getLast("navn").orElseThrow();
    }

    @Override
    public String getOrgNiv() {
        return null;
    }

    @Override
    public OrgenhetType getOrgenhetType() {
        return null;
    }

    public LocalDate getGyldigFom() {
        return LocalDate.parse(getLast("gyldigFom").orElseThrow());
    }

    public Optional<LocalDate> getGyldigTom() {
        return getLast("gyldigTom").map(LocalDate::parse);
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .map(e -> e.getHendelseData().get(property))
                .filter(Objects::nonNull)
                .map(JsonNode::asText)
                .reduce((a, b) -> b);
    }
}

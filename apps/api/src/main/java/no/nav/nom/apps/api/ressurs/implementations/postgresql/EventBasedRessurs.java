package no.nav.nom.apps.api.ressurs.implementations.postgresql;

import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Comparator.comparing;

public class EventBasedRessurs implements IRessurs {

    public final static String NAVIDENT_FIELD = "navident";
    public final static String PERSONIDENT_FIELD = "personident";
    public final static String VISNINGSFORNAVN_FIELD = "visningsfornavn";
    public final static String VISNINGSETTERNAVN_FIELD = "visningsetternavn";
    public final static String VISNINGSMELLOMNAVN_FIELD = "visningsmellomnavn";
    public final static String SEKTOR_FIELD = "sektor";
    public final static String KJONN_FIELD = "kjonn";

    private final List<IHendelse> hendelser;
    private LocalDate sluttdato;

    public EventBasedRessurs(List<IHendelse> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(h -> h.getHendelseMetadata().getOpprettetTidspunkt())).toList();
    }

    @Override
    public UUID getId() {
        return hendelser.stream()
                .findFirst()
                .map(IHendelse::getFid)
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

    @Override
    public Optional<String> getVisningsfornavn() {
        return getLast(VISNINGSFORNAVN_FIELD);
    }

    @Override
    public Optional<String> getVisningsmellomnavn() {
        return getLast(VISNINGSMELLOMNAVN_FIELD);
    }
    @Override
    public Optional<String> getVisningsetternavn() {
        return getLast(VISNINGSETTERNAVN_FIELD);
    }
    @Override
    public String getSektor() {
        return getLast(SEKTOR_FIELD).orElse(null);
    }

    @Override
    public LocalDate getSluttdato() {
        return sluttdato;
    }

    public void setSluttdato(LocalDate sluttdato){
        this.sluttdato = sluttdato;
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .filter(e -> e.getHendelseData().has(property))
                .map(e -> e.getHendelseData().get(property))
                .reduce((a, b) -> b)
                .map(e -> !e.isNull() ? e.asText() : null);
    }
}

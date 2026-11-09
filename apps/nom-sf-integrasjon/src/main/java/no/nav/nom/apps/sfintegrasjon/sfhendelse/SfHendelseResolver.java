package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import no.nav.nom.contract.nom.Tilknytningtype;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
public class SfHendelseResolver {

    private final ObjectMapper objectMapper;

    public SfHendelseResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private JsonNode hendelse;

    public SfHendelseResolver from(String sfHendelse) {
        hendelse = objectMapper.readTree(sfHendelse);
        return this;
    }

    public String navident() {
        return hendelse.path("navident").asString();
    }

    public String personident() {
        return hendelse.path("personident").asString();
    }

    public String fornavn() {
        return hendelse.path("fornavn").asString();
    }

    public String etternavn() {
        return hendelse.path("etternavn").asString();
    }

    public String visningsnavn() {
        return String.join(" ", fornavn(), etternavn());
    }

    public LocalDate startdato() {
        return toDate(hendelse.path("startdato").asString());
    }

    public LocalDate sluttDato() {
        return toDate(hendelse.path("sluttdato").asString());
    }

    public String primaryFysiskBrukersted() {
        return getDolOt().map(ot -> ot.path("primaryFysiskBrukersted").asString());
    }

    public String dolOt() {
        return getDolOt().map(orgtilknytning -> orgtilknytning.path("orgenhet").asString());
    }

    public String tilknytningtype() {
        return getDolOt().map(orgtilknytning -> orgtilknytning.path("type").asString());
    }

    public List<Stilling> sfStillinger() {
        return hendelse.path("orgtilknytninger").asArray().elements().stream()
            .map(this::from)
            .toList();
    }

    public Stilling from(JsonNode ot) {
        return new Stilling(
            new Stilling.StillingId(
                ot.path("orgenhet").asString(),
                Tilknytningtype.valueOf(ot.path("type").asString()),
                ot.path("erDagligOppfolging").asBoolean()
            ),
            otGyldigFom(ot),
            otGyldigTom(ot),
            ot.path("primaryFysiskBrukersted").asString()
        );
    }

    private JsonNode getDolOt() {
        return orgtilknytninger().stream()
            .filter(orgtilknytning -> orgtilknytning.path("erDagligOppfolging").asBoolean())
            .filter(orgtilknytning -> isAfter(otGyldigTom(orgtilknytning), startdato().minusDays(1)))
            .min(Comparator.comparing(this::otGyldigFom, Comparator.naturalOrder()))
            .orElse(null);
    }

    private Collection<JsonNode> orgtilknytninger() {
        return hendelse.path("orgtilknytninger").asArray().elements();
    }

    private boolean isAfter(LocalDate date1, LocalDate date2) {
        return date1 == null || date2 != null && date1.isAfter(date2);
    }

    private LocalDate toDate(String dateString) {
        return StringUtils.isEmpty(dateString) ? null : LocalDate.parse(dateString);
    }

    private LocalDate otGyldigFom(JsonNode ot) {
        return toDate(ot.path("gyldigFom").asString());
    }

    private LocalDate otGyldigTom(JsonNode ot) {
        return toDate(ot.path("gyldigTom").asString());

    }
}

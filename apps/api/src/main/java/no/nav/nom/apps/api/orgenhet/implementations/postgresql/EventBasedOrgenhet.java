package no.nav.nom.apps.api.orgenhet.implementations.postgresql;

import lombok.Value;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public final static String NAVN_FIELD = "navn";
    public final static String GYLDIG_FOM_FIELD = "gyldigFom";
    public final static String GYLDIG_TOM_FIELD = "gyldigTom";
    public final static String AGRESSO_ID_FIELD = "agressoId";
    public final static String AGRESSO_ORGNIV_FIELD = "agressoOrgNiv";
    public final static String AGRESSO_ORGENHET_TYPE_FIELD = "agressoOrgenhetType";

    List<IHendelse> hendelser;

    public EventBasedOrgenhet(List<IHendelse> hendelser) {
        this.hendelser = hendelser.stream()
                .sorted(comparing(h -> h.getHendelseMetadata().getOpprettetTidspunkt()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public UUID getFid() {
        return hendelser.stream().findFirst().map(IHendelse::getFid).orElseThrow();
    }

    @Override
    public String getAgressoId() {
        return getLast(AGRESSO_ID_FIELD).orElseThrow();
    }

    @Override
    public String getAgressoOrgenhetType() {
        return getLast(AGRESSO_ORGENHET_TYPE_FIELD).orElse(null);
    }

    @Override
    public String getNavn() {
        return getLast(NAVN_FIELD).orElseThrow();
    }

    @Override
    public String getOrgNiv() {
        return getLast(AGRESSO_ORGNIV_FIELD).orElseThrow();
    }

    public LocalDate getGyldigFom() {
        return LocalDate.parse(getLast(GYLDIG_FOM_FIELD).orElseThrow());
    }

    public Optional<LocalDate> getGyldigTom() {
        return getLast(GYLDIG_TOM_FIELD).map(LocalDate::parse);
    }

    @Override
    public Optional<OrgEnhetsKategori> getOrgEnhetsKategori() {
        return Optional.ofNullable(StaticOrgenhetkategoriMapping.resolveFor(getAgressoId(), getOrgNiv()));
    }

    @Override
    public String getUniqueAgressoId() {
        return getAgressoId() + "-" + getNavn() + "-" + getOrgNiv();
    }

    private Optional<String> getLast(String property) {
        return hendelser.stream()
                .filter(e -> e.getHendelseData().has(property))
                .map(e -> e.getHendelseData().get(property))
                .reduce((a, b) -> b)
                .map(e -> !e.isNull() ? e.asText() : null);
    }

    public void addHendelse(IHendelse hendelse) {
        hendelser.add(hendelse);
    }

    public List<IHendelse> getHendelser() {
        return Collections.unmodifiableList(hendelser);
    }

    private class StaticOrgenhetkategoriMapping {
        static OrgEnhetsKategori resolveFor(String agressoId, String orgniv) {
            return switch (orgniv) {
                case "ORGNIV1" -> {
                    if ("1".equals(agressoId) || "4".equals(agressoId)) {
                        yield OrgEnhetsKategori.LINJEENHET;
                    } else if ("80".equals(agressoId)) {
                        yield OrgEnhetsKategori.DRIFTSENHET;
                    } else {
                        yield null;
                    }
                }
                case "ORGNIV2" -> {
                    if ("821".equals(agressoId)) {
                        yield OrgEnhetsKategori.ARBEIDSOMRAADE;
                    } else if ("48".equals(agressoId) || "R10".equals(agressoId)) {
                        yield OrgEnhetsKategori.DRIFTSENHET;
                    } else {
                        yield null;
                    }
                }
                case "ORGNIV3" -> {
                    if ("480600".equals(agressoId) || "090600".equals(agressoId)) {
                        yield OrgEnhetsKategori.ARBEIDSOMRAADE;
                    } else {
                        yield null;
                    }
                }
                default -> null;
            };
        }
    }
}
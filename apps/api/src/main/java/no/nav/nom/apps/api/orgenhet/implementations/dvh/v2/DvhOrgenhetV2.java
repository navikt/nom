package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.libs.utils.common.util.DateValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Data
@NoArgsConstructor
public class DvhOrgenhetV2 implements IOrgenhet {
    private String agressoId;
    private String orgNiv;
    private String navn;
    private String agressoOrgenhetType;
    private String agressoGyldigFom;
    private String agressoGyldigTom;
    private String orgenhetStatus;
    private LocalDate stillingMaksDato;
    private LocalDate dvhLastetDato;
    private String lederRessursNr;
    private JsonNode rawData;

    private static final String FIRST_DAY_OF_MONTH = "01";
    private final static LocalDate AGRESSO_MAX_DATE = LocalDate.parse("2099-12-31");
    private final static DateTimeFormatter yyyyMMdd_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final static DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateValidator dateValidator = new DateValidator(yyyyMMdd_FORMATTER);

    @Override
    public String getFid() {
        return null;
    }
    @Override
    public String getId() {
        return null;
    }

    @Override
    public LocalDate getGyldigFom() {
        return !agressoGyldigFom.equals("0") && dateValidator.isValid(agressoGyldigFom + FIRST_DAY_OF_MONTH) ? getFirstDay(agressoGyldigFom) : dvhLastetDato;
    }

    @Override
    public LocalDate getGyldigTom() {
        // Alle Enheter !"ORGENHET" har alltid agressoGyldigFom="0" && agressoGyldigTom = "209912" => Så alltid return empty()
        // "ORGENHET" har aldri agressoGyldigFom="0"
        if (!"ORGENHET".equals(orgNiv)) {
            return null;
        }

        final var agressoGyldigTomDato = getDate(getLastDay(agressoGyldigTom)); // getDate returnerer null hvis Max dato (2099-12-31)
        if (nonNull(agressoGyldigTomDato)) {
            return agressoGyldigTomDato;
        }

        final var useMaxStillingDate = "P".equals(orgenhetStatus) && "ORGENHET".equals(orgNiv);
        return useMaxStillingDate ? getDate(stillingMaksDato) : null;
    }

    @Override
    public NomNivaa getNomNivaa() {
        return null;
    }

    @Override
    public OrgEnhetsType getOrgEnhetsType() {
        return null;
    }

    @Override
    public String getUniqueAgressoId() {
        return getAgressoId() + "-" + getNavn() + "-" + getOrgNiv();
    }

    private LocalDate getLastDay(String yyyyMM) {
        if (yyyyMM == null) {
            return null;
        }
        LocalDate convertedDate = getFirstDay(yyyyMM);
        return convertedDate.withDayOfMonth(convertedDate.getMonth().length(convertedDate.isLeapYear()));
    }

    private LocalDate getFirstDay(String yyyyMM) {
        return LocalDate.parse(yyyyMM + FIRST_DAY_OF_MONTH, yyyyMMdd_FORMATTER);
    }

    private LocalDate getDate(LocalDate date) {
        if (isNull(date) || date.equals(AGRESSO_MAX_DATE)) {
            return null;
        }
        return date;
    }

    public DvhOrgenhetV2 merge(DvhOrgenhetV2 dvhOrgenhetV2, JsonMapper jsonMapper) {
        if (getGyldigFom().isAfter(dvhOrgenhetV2.getGyldigFom())) {
            this.agressoGyldigFom = dvhOrgenhetV2.getAgressoGyldigFom();
        }
        if (Objects.requireNonNullElse(getGyldigTom(), LocalDate.MAX).isBefore(Objects.requireNonNullElse(dvhOrgenhetV2.getGyldigTom(), LocalDate.MAX))) {
            this.agressoGyldigTom = dvhOrgenhetV2.getAgressoGyldigTom();
        }
        if (dvhOrgenhetV2.orgenhetStatus.equals("N")) {
            this.orgenhetStatus = dvhOrgenhetV2.orgenhetStatus;
        }

        if (!this.getRawData().isArray()) {
            this.setRawData(jsonMapper.createArrayNode().add(this.getRawData()));
        }
        ((ArrayNode) this.getRawData()).add(dvhOrgenhetV2.getRawData());

        return this;
    }

    @Override
    public String getOverordnetOrgEnhetFid() {
        return null;
    }

    @Override
    public String getTilgangsenhetId() {
        return null;
    }

    public static DvhOrgenhetV2 fromOrds(JsonNode node) {
        var enhet = new DvhOrgenhetV2();
        enhet.setRawData(node);
        enhet.setAgressoId(node.get("orgenhet_kode").asString());
        enhet.setOrgNiv(node.get("orgnode_niv").asString());
        enhet.navn = node.get("orgenhet_navn").asString();
        enhet.agressoOrgenhetType = node.get("enhettype_navn").asString(null);
        enhet.agressoGyldigFom = node.get("orgenhet_fra_dato").asString();
        val tempGyldigTom = node.get("orgenhet_til_dato").asString();
        enhet.agressoGyldigTom = tempGyldigTom.equals("209999") ? "209912" : tempGyldigTom;
        enhet.orgenhetStatus = node.get("orgenhet_status").asString().toUpperCase();
        enhet.stillingMaksDato = node.hasNonNull("stilling_maks_gyldig_til_dato") ?
            localDateOf(node.get("stilling_maks_gyldig_til_dato").asString()) : null;
        enhet.dvhLastetDato = localDateOf(node.get("lastet_dato").asString());
        enhet.lederRessursNr = node.hasNonNull("leder_ressursnr") ? node.get("leder_ressursnr").asString() : null;

        // Hvis stillingMaksDato er før Agresso sin gyldigFom dato, er den nok knyttet
        // til en tidligere enhet med samme agressoId.
        if (enhet.getStillingMaksDato() != null && enhet.getGyldigFom().isAfter(enhet.getStillingMaksDato())) {
            enhet.setStillingMaksDato(null);
        }
        return enhet;
    }

    public static LocalDate localDateOf(String date) {
        return LocalDate.ofInstant(Instant.parse(date), ZoneId.of("Europe/Oslo")); // timstamped datoer fra ORDS er i Oslo tid levert som UTC, E.g datoen 2025-12-31 leveres som 2025-12-30T23:00:00Z
    }
}

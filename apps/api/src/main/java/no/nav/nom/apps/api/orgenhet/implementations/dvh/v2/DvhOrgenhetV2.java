package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.libs.utils.common.util.DateValidator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import static java.util.function.Predicate.not;

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

    public DvhOrgenhetV2(JsonNode node) {
        this.rawData = node;
        this.agressoId = node.get("ORGENHET_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIV").asText();
        this.navn = node.get("ORGENHET_NAVN").asText();
        this.agressoOrgenhetType = node.get("ENHETTYPE_NAVN").asText(null);
        this.agressoGyldigFom = node.get("ORGENHET_FRA_PERIODE").asText();
        val tempGyldigTom = node.get("ORGENHET_TIL_PERIODE").asText();
        this.agressoGyldigTom = tempGyldigTom.equals("209999") ? "209912" : tempGyldigTom;
        this.orgenhetStatus = node.get("ORGENHET_STATUS").asText().toUpperCase();
        this.stillingMaksDato = node.hasNonNull("STILLING_MAKS_GYLDIG_TIL_DATO") ?
            LocalDate.parse(node.get("STILLING_MAKS_GYLDIG_TIL_DATO").asText(), YYYY_MM_DD_FORMATTER) : null;
        this.dvhLastetDato = LocalDate.parse(node.get("LASTET_DATO").asText());
        this.lederRessursNr = node.hasNonNull("LEDER_RESSURSNR") ? node.get("LEDER_RESSURSNR").asText() : null;

        // Hvis stillingMaksDato er før Agresso sin gyldigFom dato, er den nok knyttet
        // til en tidligere enhet med samme agressoId.
        if (stillingMaksDato != null && getGyldigFom().isAfter(stillingMaksDato)) {
            stillingMaksDato = null;
        }
    }

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
    public Optional<LocalDate> getGyldigTom() {
        // Alle Enheter !"ORGENHET" har alltid agressoGyldigFom="0" && agressoGyldigTom = "209912" => Så alltid return empty()
        // "ORGENHET" har aldri agressoGyldigFom="0"
        if (!"ORGENHET".equals(orgNiv)) {
            return Optional.empty();
        }

        final var agressoGyldigTomDato = getDate(getLastDay(agressoGyldigTom)); // getDate returnerer Optional.Empty hvis Max dato (2099-12-31)
        if (agressoGyldigTomDato.isPresent()) {
            return agressoGyldigTomDato;
        }

        final var useMaxStillingDate = "P".equals(orgenhetStatus) && "ORGENHET".equals(orgNiv);
        return useMaxStillingDate ? getDate(stillingMaksDato) : Optional.empty();
    }

    @Override
    public Optional<NomNivaa> getNomNivaa() {
        return Optional.empty();
    }

    @Override
    public Optional<OrgEnhetsType> getOrgEnhetsType() {
        return Optional.empty();
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

    private Optional<LocalDate> getDate(LocalDate date) {
        return Optional.ofNullable(date).filter(not(AGRESSO_MAX_DATE::isEqual));
    }

    public DvhOrgenhetV2 merge(DvhOrgenhetV2 dvhOrgenhetV2, ObjectMapper objectMapper) {
        if (getGyldigFom().isAfter(dvhOrgenhetV2.getGyldigFom())) {
            this.agressoGyldigFom = dvhOrgenhetV2.getAgressoGyldigFom();
        }
        if (getGyldigTom().orElse(LocalDate.MAX).isBefore(dvhOrgenhetV2.getGyldigTom().orElse(LocalDate.MAX))) {
            this.agressoGyldigTom = dvhOrgenhetV2.getAgressoGyldigTom();
        }
        if (dvhOrgenhetV2.orgenhetStatus.equals("N")) {
            this.orgenhetStatus = dvhOrgenhetV2.orgenhetStatus;
        }

        if (!this.getRawData().isArray()) {
            this.setRawData(objectMapper.createArrayNode().add(this.getRawData()));
        }
        ((ArrayNode) this.getRawData()).add(dvhOrgenhetV2.getRawData());

        return this;
    }

    @Override
    public Optional<String> getOverordnetOrgEnhetFid() {
        return Optional.empty();
    }

    public static DvhOrgenhetV2 fromOrds(JsonNode node) {
        var enhet = new DvhOrgenhetV2();
        enhet.setRawData(node);
        enhet.setAgressoId(node.get("orgenhet_kode").asText());
        enhet.setOrgNiv(node.get("orgnode_niv").asText());
        enhet.navn = node.get("orgenhet_navn").asText();
        enhet.agressoOrgenhetType = node.get("enhettype_navn").asText(null);
        enhet.agressoGyldigFom = node.get("orgenhet_fra_dato").asText();
        val tempGyldigTom = node.get("orgenhet_til_dato").asText();
        enhet.agressoGyldigTom = tempGyldigTom.equals("209999") ? "209912" : tempGyldigTom;
        enhet.orgenhetStatus = node.get("orgenhet_status").asText().toUpperCase();
        enhet.stillingMaksDato = node.hasNonNull("stilling_maks_gyldig_til_dato") ?
            localDateOf(node.get("stilling_maks_gyldig_til_dato").asText()) : null;
        enhet.dvhLastetDato = localDateOf(node.get("lastet_dato").asText());
        enhet.lederRessursNr = node.hasNonNull("leder_ressursnr") ? node.get("leder_ressursnr").asText() : null;

        // Hvis stillingMaksDato er før Agresso sin gyldigFom dato, er den nok knyttet
        // til en tidligere enhet med samme agressoId.
        if (enhet.getStillingMaksDato() != null && enhet.getGyldigFom().isAfter(enhet.getStillingMaksDato())) {
            enhet.setStillingMaksDato(null);
        }
        return enhet;
    }

    public static LocalDate localDateOf(String date) {
        return LocalDate.ofInstant(Instant.parse(date), ZoneId.systemDefault());
    }

    public static boolean compare(DvhOrgenhetV2 tdvOrgenhet, DvhOrgenhetV2 ordsOrgenhet) {
        return Objects.equals(tdvOrgenhet.getAgressoId(),ordsOrgenhet.getAgressoId()) &&
            Objects.equals(tdvOrgenhet.getOrgNiv(),ordsOrgenhet.getOrgNiv()) &&
            Objects.equals(tdvOrgenhet.getNavn(),ordsOrgenhet.getNavn()) &&
            Objects.equals(tdvOrgenhet.getAgressoOrgenhetType(),ordsOrgenhet.getAgressoOrgenhetType()) &&
            Objects.equals(tdvOrgenhet.getAgressoGyldigFom(),ordsOrgenhet.getAgressoGyldigFom()) &&
            Objects.equals(tdvOrgenhet.getAgressoGyldigTom(),ordsOrgenhet.getAgressoGyldigTom()) &&
            Objects.equals(tdvOrgenhet.getOrgenhetStatus(),ordsOrgenhet.getOrgenhetStatus()) &&
            Objects.equals(tdvOrgenhet.getStillingMaksDato(),ordsOrgenhet.getStillingMaksDato()) &&
            Objects.equals(tdvOrgenhet.getDvhLastetDato(),ordsOrgenhet.getDvhLastetDato()) &&
            Objects.equals(tdvOrgenhet.getLederRessursNr(),ordsOrgenhet.getLederRessursNr());
    }
}

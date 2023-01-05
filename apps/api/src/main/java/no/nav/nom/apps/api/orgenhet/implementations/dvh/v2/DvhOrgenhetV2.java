package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.val;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgEnhetsKategori;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static java.util.function.Predicate.not;

@Data
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

    private final static LocalDate AGRESSO_MAX_DATE = LocalDate.parse("2099-12-31");
    private final static DateTimeFormatter yyyyMMdd_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final static DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
    public UUID getFid() {
        return null;
    }

    @Override
    public LocalDate getGyldigFom() {
        return !agressoGyldigFom.equals("0") ? getFirstDay(agressoGyldigFom) : dvhLastetDato;
    }

    @Override
    public Optional<LocalDate> getGyldigTom() {
        final var ignoreDate = !"ORGENHET".equals(orgNiv) || "0".equals(agressoGyldigFom) || "209999".equals(agressoGyldigTom);
        if (ignoreDate) {
            return Optional.empty();
        }

        final var agressoGyldigTomDato = getDate(getLastDay(agressoGyldigTom));
        if (agressoGyldigTomDato.isPresent()) {
            return agressoGyldigTomDato;
        }

        final var useMaxStillingDate = "P".equals(orgenhetStatus) && "ORGENHET".equals(orgNiv);
        return useMaxStillingDate ? getDate(stillingMaksDato) : Optional.empty();
    }

    @Override
    public Optional<OrgEnhetsKategori> getOrgEnhetsKategori() {
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
        return LocalDate.parse(yyyyMM + "01", yyyyMMdd_FORMATTER);
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
}

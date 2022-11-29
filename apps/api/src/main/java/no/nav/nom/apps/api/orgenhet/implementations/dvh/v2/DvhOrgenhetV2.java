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

    public DvhOrgenhetV2(JsonNode node) {
        this.rawData = node;
        this.agressoId = node.get("ORGENHET_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIV").asText();
        this.navn = node.get("ORGENHET_NAVN").asText();
        this.agressoOrgenhetType = node.get("ENHETTYPE_NAVN").asText();
        this.agressoGyldigFom = node.get("ORGENHET_FRA_PERIODE").asText();
        val tempGyldigTom = node.get("ORGENHET_TIL_PERIODE").asText();
        this.agressoGyldigTom = tempGyldigTom.equals("209999") ? "209912" : tempGyldigTom;
        this.orgenhetStatus = node.get("ORGENHET_STATUS").asText().toUpperCase();
        this.stillingMaksDato = node.hasNonNull("STILLING_MAKS_GYLDIG_TIL_DATO") ?
                LocalDate.parse(node.get("STILLING_MAKS_GYLDIG_TIL_DATO").asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
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
        val dateToUse = !orgNiv.equals("ORGENHET") || agressoGyldigFom.equals("0") || agressoGyldigTom.equals("209999") ? null : agressoGyldigTom;
        return Optional.ofNullable(getDate(getLastDay(dateToUse)).orElseGet(() -> orgenhetStatus.equals("P") && orgNiv.equals("ORGENHET") ?
                getDate(stillingMaksDato).orElse(null) : null));
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
        if (yyyyMM != null) {
            LocalDate convertedDate = getFirstDay(yyyyMM);
            return convertedDate.withDayOfMonth(convertedDate.getMonth().length(convertedDate.isLeapYear()));
        }
        return null;
    }

    private LocalDate getFirstDay(String yyyyMM) {
        return LocalDate.parse(yyyyMM + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private Optional<LocalDate> getDate(LocalDate date) {
        if (date != null && !date.isEqual(LocalDate.parse("2099-12-31"))) {
            return Optional.of(date);
        }
        return Optional.empty();
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

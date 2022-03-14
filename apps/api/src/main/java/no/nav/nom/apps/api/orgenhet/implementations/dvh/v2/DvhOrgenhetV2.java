package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;
import static no.nav.nom.apps.api.orgenhet.implementations.dvh.DvhOrgenhetTypeMapper.agressoToNomType;

@Value
public class DvhOrgenhetV2 implements IOrgenhet {
    String agressoId;
    String orgNiv;
    String navn;
    String agressoOrgType;
    String gyldigFom;
    String gyldigTom;
    String orgenhetStatus;
    LocalDate stillingMaksDato;

    public DvhOrgenhetV2(String agressoId, String orgNiv, String navn, String agressoOrgType, String gyldigFom, String gyldigTom, String orgenhetStatus, LocalDate stillingMaksDato){
        this.agressoId = agressoId;
        this.orgNiv = orgNiv;
        this.navn = navn;
        this.agressoOrgType = agressoOrgType;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
        this.orgenhetStatus = orgenhetStatus;
        this.stillingMaksDato = stillingMaksDato;
    }

    public DvhOrgenhetV2(JsonNode node) {
        this.agressoId = node.get("ORGENHET_KODE").asText();
        this.orgNiv = "ORGENHET";
        this.navn = node.get("ORGENHET_NAVN").asText();
        this.agressoOrgType = "11"; //node.get("agresso_orgnode_type").asText();
        this.gyldigFom = node.get("ORGENHET_FRA_PERIODE").asText();
        this.gyldigTom = node.get("ORGENHET_TIL_PERIODE").asText();
        this.orgenhetStatus = node.get("ORGENHET_STATUS").asText().toUpperCase();
        this.stillingMaksDato = node.hasNonNull("STILLING_MAKS_GYLDIG_TIL_DATO") ? LocalDate.parse(node.get("STILLING_MAKS_GYLDIG_TIL_DATO").asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
    }

    @Override
    public OrgenhetType getOrgenhetType(){
        return agressoToNomType(agressoOrgType);
    }

    @Override
    public LocalDate getGyldigFom() {
        return getFirstDay(gyldigFom);
    }

    @Override
    public Optional<LocalDate> getGyldigTom(){
        return Optional.ofNullable(
                getDate(getLastDay(gyldigTom)).orElseGet(() ->
                        orgenhetStatus.equals("P") ? getDate(stillingMaksDato).orElse(null) : null
                )
        );
    }

    private LocalDate getLastDay(String yyyyMM){
        LocalDate convertedDate = getFirstDay(yyyyMM);
        return convertedDate.withDayOfMonth(convertedDate.getMonth().length(convertedDate.isLeapYear()));
    }

    private LocalDate getFirstDay(String yyyyMM){
        return LocalDate.parse(yyyyMM+"01", DateTimeFormatter.ofPattern("yyyyMMdd"));

    }

    private Optional<LocalDate> getDate(LocalDate date){
        if(date != null && !date.isEqual(LocalDate.parse("2099-12-31"))){
            return Optional.of(date);
        }
        return Optional.empty();
    }
}

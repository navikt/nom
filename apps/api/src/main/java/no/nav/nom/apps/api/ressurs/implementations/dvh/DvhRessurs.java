package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.IdentType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.ANNEN_STATLIG;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.EKSTERN;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.KOMMUNAL;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.STATLIG;

@Value
@Builder
@RequiredArgsConstructor
public class DvhRessurs {

    private static final LocalDate D2099 = LocalDate.of(2099, 12, 31);


    String navIdent;
    String personIdent;
    String fornavn;
    String etternavn;
    String remedyAnsattType;
    Boolean remedyAktiv;
    JsonNode rawData;
    String ansattRessursKode;
    LocalDate remedyStartDato;
    LocalDate remedySluttdato;
    LocalDate agressoStartDato;
    LocalDate agressoSluttdato;
    boolean remedyPermisjon;
    String identType;
    String epost;

    public DvhRessurs(JsonNode node) {
        navIdent = node.get("NAV_IDENT").asText();
        personIdent = node.get("FNR").asText();
        fornavn = node.get("FORNAVN").asText();
        etternavn = node.get("ETTERNAVN").asText();
        remedyAktiv = node.hasNonNull("REMEDY_AKTIV") && node.get("REMEDY_AKTIV").asText().equals("JA");
        remedyAnsattType = node.get("ANSATT_TYPE").asText();
        this.rawData = node;
        ansattRessursKode = node.get("ANSATT_RESSURS_KODE").asText();
        remedyStartDato = parseNullableDateFromJson(node, "START_DATO");
        remedySluttdato = parseNullableDateFromJson(node, "REMEDY_SLUTT_DATO");
        agressoStartDato = parseNullableDateFromJson(node, "AGRESSO_START_DATO");
        agressoSluttdato = parseNullableDateFromJson(node, "AGRESSO_SLUTT_DATO");
        remedyPermisjon = node.get("REMEDY_PERMISJON").asText("NEI").equals("JA");
        identType = node.get("IDENT_TYPE").asText();
        epost = node.get("EPOST").asText();

    }

    public enum RemedyRessursType {
        EKSTERN("Eksternt"),
        STATLIG("NAV Statlig"),
        KOMMUNAL("NAV Kommunalt"),
        ANNEN_STATLIG("Annen Statlig");

        public final String beskrivelse;

        RemedyRessursType(String beskrivelse) {
            this.beskrivelse = beskrivelse;
        }
    }

    public LocalDate getSluttdato() {
        if (!hasPermisjon()) {
            if ("E".equals(ansattRessursKode) || Arrays.asList(EKSTERN.beskrivelse, ANNEN_STATLIG.beskrivelse, KOMMUNAL.beskrivelse)
                    .contains(remedyAnsattType)) {
                return getDateOrNull(remedySluttdato);
            }

            return agressoSluttdato != null && !D2099.equals(agressoSluttdato) ? agressoSluttdato : getDateOrNull(remedySluttdato);
        }
        return null;
    }

    public LocalDate getStartDato() {
        var isStatlig = Arrays.asList(STATLIG.beskrivelse).contains(this.remedyAnsattType);
        if(isStatlig && this.agressoStartDato != null){
            return this.agressoStartDato;
        }else{
            return this.remedyStartDato;
        }
    }

    public boolean hasPermisjon() {
        if (remedyPermisjon && remedyAktiv) {
            if (List.of(KOMMUNAL.beskrivelse, STATLIG.beskrivelse).contains(remedyAnsattType)) {
                if (KOMMUNAL.beskrivelse.equals(remedyAnsattType)) {
                    return true;
                }
                if (agressoSluttdato == null || LocalDate.of(2099, 12, 31).equals(agressoSluttdato)) {
                    return true;
                }
            }
        }
        return false;
    }

    private LocalDate getDateOrNull(final LocalDate dato) {
        return !D2099.equals(dato) ? dato : null;
    }

    public static Sektor toSektor(String remedyAnsattType) {
        if (remedyAnsattType.equals("NAV Statlig")) {
            return Sektor.NAV_STATLIG;
        } else if (remedyAnsattType.equals("NAV Kommunalt")) {
            return Sektor.NAV_KOMMUNAL;
        } else {
            return Sektor.EKSTERN;
        }
    }

    public static IdentType toIdentType(String identType){

        for(var e : IdentType.values()){
            if(e.name().equals(identType)){
                return e;
            }
        }
        throw new IllegalArgumentException(identType + " was not recongized as an enum value of " + IdentType.class.getName());
    }

    private static LocalDate parseNullableDateFromJson(JsonNode jsonNode, String s){
        return jsonNode.hasNonNull(s) ? LocalDate.parse(jsonNode.get(s).asText()) : null;
    }
}

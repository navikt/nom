package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Value
@Builder
@RequiredArgsConstructor
public class DvhRessurs implements IRessurs {

    private static final LocalDate D2099 = LocalDate.of(2099, 12, 31);


    String navIdent;
    String personIdent;
    String remedyAnsattType;
    Boolean remedyAktiv;
    JsonNode rawData;
    String ansattRessursKode;
    LocalDate remedySluttdato;
    LocalDate agressoSluttdato;
    boolean remedyPermisjon;

    public DvhRessurs(JsonNode node) {
        navIdent = node.get("NAV_IDENT").asText();
        personIdent = node.get("FNR").asText();
        remedyAktiv = node.hasNonNull("REMEDY_AKTIV") && node.get("REMEDY_AKTIV").asText().equals("JA");
        remedyAnsattType = node.get("ANSATT_TYPE").asText();
        this.rawData = node;
        ansattRessursKode = node.get("ANSATT_RESSURS_KODE").asText();
        remedySluttdato = node.hasNonNull("REMEDY_SLUTT_DATO") ? LocalDate.parse(node.get("REMEDY_SLUTT_DATO").asText()) : null;
        agressoSluttdato = node.hasNonNull("AGRESSO_SLUTT_DATO") ? LocalDate.parse(node.get("AGRESSO_SLUTT_DATO").asText()) : null;
        remedyPermisjon = node.get("REMEDY_PERMISJON").asText("NEI").equals("JA");
    }

    @Override
    public UUID getId() {
        return null;
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

    public LocalDate getSluttdato(){
        if(!hasPermisjon()) {
            if ("E".equals(ansattRessursKode) ||
                    Arrays.asList(RemedyRessursType.EKSTERN.beskrivelse, RemedyRessursType.ANNEN_STATLIG.beskrivelse, RemedyRessursType.KOMMUNAL.beskrivelse).contains(remedyAnsattType)) {
                return getDateOrNull(remedySluttdato);
            }

            return agressoSluttdato != null && !D2099.equals(agressoSluttdato)
                    ? agressoSluttdato : getDateOrNull(remedySluttdato);
        }
        return null;
    }

    public boolean hasPermisjon(){
        if(remedyPermisjon && remedyAktiv){
            if(List.of(RemedyRessursType.KOMMUNAL.beskrivelse, RemedyRessursType.STATLIG.beskrivelse).contains(remedyAnsattType)){
                if (RemedyRessursType.KOMMUNAL.beskrivelse.equals(remedyAnsattType)) {
                    return true;
                }
                if(agressoSluttdato == null || LocalDate.of(2099, 12, 31).equals(agressoSluttdato)){
                    return true;
                }
            }
        }
        return false;
    }

    private LocalDate getDateOrNull(final LocalDate dato) {
        return !D2099.equals(dato) ? dato : null;
    }
}

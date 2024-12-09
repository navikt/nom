package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    String orgenhetPaaRessurs;
    String orgenhetPaaRessursBeskrivelse;
    String orgenhetPaaRessursNiva;
    String fysiskBrukersted;

    public DvhRessurs(JsonNode node) {
        this(node, false);
    }

    public DvhRessurs(JsonNode node, boolean fromNomKommunaleView) {
        if (!fromNomKommunaleView) { // Good old Json fra NOM_ANSATTE_BUCKETS
            navIdent = node.get("NAV_IDENT").asText();
            personIdent = node.get("FNR").asText();
            fornavn = node.get("FORNAVN").asText();
            etternavn = node.get("ETTERNAVN").asText();
            remedyAktiv = node.hasNonNull("REMEDY_AKTIV") && node.get("REMEDY_AKTIV").asText().equals("JA");
            remedyAnsattType = node.get("ANSATT_TYPE").asText();
            this.rawData = node;
            ansattRessursKode = node.get("ANSATT_RESSURS_KODE").asText();
            remedyStartDato = parseNullableDateFromJson(node, "REMEDY_START_DATO");
            remedySluttdato = parseNullableDateFromJson(node, "REMEDY_SLUTT_DATO");
            agressoStartDato = parseNullableDateFromJson(node, "AGRESSO_START_DATO");
            agressoSluttdato = parseNullableDateFromJson(node, "AGRESSO_SLUTT_DATO");
            remedyPermisjon = node.get("REMEDY_PERMISJON").asText("NEI").equals("JA");
            identType = node.get("IDENT_TYPE").asText();
            epost = node.get("EPOST").asText();
            fysiskBrukersted = parseNullableFromJson(node, "FYSISK_BRUKERSTED");

            orgenhetPaaRessurs = null;
            orgenhetPaaRessursBeskrivelse = null;
            orgenhetPaaRessursNiva = null;

        } else { // Ny JSON fra NOM_KOMMUNALE_V1
            navIdent = node.get("NAVID").asText();
            personIdent = node.get("FNR").asText();
            fornavn = node.get("NAVN").asText();
            etternavn = node.get("ETTERNAVN").asText();
            remedyAktiv = node.hasNonNull("AKTIV") && node.get("AKTIV").asText().equals("JA");
            remedyAnsattType = node.get("ANSATT_TYPE").asText();
            this.rawData = node;
            ansattRessursKode = node.get("TYPE").asText();
            remedyStartDato = parseNullableDateFromJson(node, "START_DATO");
            remedySluttdato = parseNullableDateFromJson(node, "SLUTT_DATO");
            agressoStartDato = parseNullableDateFromJson(node, "DATE_FROM");
            agressoSluttdato = parseNullableDateFromJson(node, "DATE_TO");
            remedyPermisjon = node.get("PERMISJON").asText("NEI").equals("JA");
            identType = node.get("IDENTTYPE").asText();
            epost = node.get("EPOST").asText();
            fysiskBrukersted = parseNullableFromJson(node, "FYSISK_BRUKERSTED");

            // Kommer med i spørring på kommunale (Hr/NOM3/NOM_KOMMUNALE_V1)
            orgenhetPaaRessurs = parseNullableFromJson(node, "ORGENHET_PAA_RESSURS");
            orgenhetPaaRessursBeskrivelse = parseNullableFromJson(node, "ORGENHET_PAA_RESSURS_BESKRIVELSE");
            orgenhetPaaRessursNiva = parseNullableFromJson(node, "ORGENHET_PAA_RESSURS_NIVA");
        }
    }

    public static DvhRessurs fromOrdsRessurs(JsonNode ordsRessurs) {
        return DvhRessurs.builder()
            .navIdent(ordsRessurs.get("nav_ident").asText())
            .personIdent(ordsRessurs.get("fnr").asText())
            .fornavn(ordsRessurs.get("fornavn").asText())
            .etternavn(ordsRessurs.get("etternavn").asText())
            .remedyAktiv(ordsRessurs.hasNonNull("remedy_aktiv") && ordsRessurs.get("remedy_aktiv").asText().equals("JA"))
            .remedyAnsattType(ordsRessurs.get("ansatt_type").asText())
            .rawData(ordsRessurs)
            .ansattRessursKode(ordsRessurs.get("ansatt_ressurs_kode").asText())
            .remedyStartDato(parseNullableDateFromJson(ordsRessurs, "remedy_start_dato"))
            .remedySluttdato(parseNullableDateFromJson(ordsRessurs, "remedy_slutt_dato"))
            .agressoStartDato(parseNullableDateFromJson(ordsRessurs, "agresso_start_dato"))
            .agressoSluttdato(parseNullableDateFromJson(ordsRessurs, "agresso_slutt_dato"))
            .remedyPermisjon(ordsRessurs.get("remedy_permisjon").asText("NEI").equals("JA"))
            .identType(ordsRessurs.get("ident_type").asText())
            .epost(ordsRessurs.get("epost").asText())
            .fysiskBrukersted(parseNullableFromJson(ordsRessurs, "fysisk_brukersted"))
            .build();

        // orgenhetPaaRessurs = null;
        // genhetPaaRessursBeskrivelse = null;
        // orgenhetPaaRessursNiva = null;
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


    private boolean withinSixMonths(LocalDate d1, LocalDate d2){
        var stepLen = 6/2;
        var bound1 = d1.plusMonths(stepLen).isAfter(d2.minusMonths(stepLen));
        var bound2 = d1.minusMonths(stepLen).isBefore(d2.plusMonths(stepLen));
        return bound1 && bound2;
    }

    public LocalDate getStartDato() {
        if (this.agressoStartDato != null) {
            var u4StartBeforeRemedy = this.agressoStartDato.getYear() < 2006;
            var isStatlig = Arrays.asList(STATLIG.beskrivelse).contains(this.remedyAnsattType);
            var inBounds = withinSixMonths(this.agressoStartDato, this.remedyStartDato);
            if (u4StartBeforeRemedy || (inBounds && isStatlig)) {
                return this.agressoStartDato;
            }
        }
        return this.remedyStartDato;
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

    private static String parseNullableFromJson(JsonNode jsonNode, String s){
        return jsonNode.hasNonNull(s) ? jsonNode.get(s).asText() : null;
    }

    public static boolean compare(DvhRessurs dvhRessursTdv, DvhRessurs dvhRessursOrds) {
        return Objects.equals(dvhRessursTdv.getNavIdent(), dvhRessursOrds.getNavIdent())
            && Objects.equals(dvhRessursTdv.getPersonIdent(), dvhRessursOrds.getPersonIdent())
            && Objects.equals(dvhRessursTdv.getFornavn(), dvhRessursOrds.getFornavn())
            && Objects.equals(dvhRessursTdv.getEtternavn(), dvhRessursOrds.getEtternavn())
            && Objects.equals(dvhRessursTdv.getRemedyAktiv(), dvhRessursOrds.getRemedyAktiv())
            && Objects.equals(dvhRessursTdv.getRemedyAnsattType(), dvhRessursOrds.getRemedyAnsattType())
            && Objects.equals(dvhRessursTdv.getRemedyStartDato(), dvhRessursOrds.getRemedyStartDato())
            && Objects.equals(dvhRessursTdv.getRemedySluttdato(), dvhRessursOrds.getRemedySluttdato())
            && Objects.equals(dvhRessursTdv.getAgressoStartDato(), dvhRessursOrds.getAgressoStartDato())
            && Objects.equals(dvhRessursTdv.getAgressoSluttdato(), dvhRessursOrds.getAgressoSluttdato())
            && Objects.equals(dvhRessursTdv.getIdentType(), dvhRessursOrds.getIdentType())
            && Objects.equals(dvhRessursTdv.getEpost(), dvhRessursOrds.getEpost())
            && Objects.equals(dvhRessursTdv.getFysiskBrukersted(), dvhRessursOrds.getFysiskBrukersted())
            && Objects.equals(dvhRessursTdv.getOrgenhetPaaRessurs(), dvhRessursOrds.getOrgenhetPaaRessurs())
            && Objects.equals(dvhRessursTdv.getOrgenhetPaaRessursBeskrivelse(), dvhRessursOrds.getOrgenhetPaaRessursBeskrivelse())
            && Objects.equals(dvhRessursTdv.getOrgenhetPaaRessursNiva(), dvhRessursOrds.getOrgenhetPaaRessursNiva());
    }
}

package no.nav.nom.apps.api.ressurs.implementations.dvh;

import lombok.Builder;
import lombok.Value;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.Sektor;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.ANNEN_STATLIG;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.EKSTERN;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.KOMMUNAL;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.STATLIG;

@Value
@Builder
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
    String ansettelsesTypeBesk;
    LocalDate remedyStartDato;
    LocalDate remedySluttdato;
    LocalDate agressoStartDato;
    LocalDate agressoSluttdato;
    Boolean agressoAnsattIDag;      // Agressobruker start/slutt ift dagens dato
    boolean remedyPermisjon;
    String identType;
    String epost;
    String orgenhetPaaRessurs;
    String orgenhetPaaRessursBeskrivelse;
    String orgenhetPaaRessursNiva;
    String fysiskBrukersted;

    public static DvhRessurs fromOrdsRessurs(JsonNode ordsRessurs) {
        return DvhRessurs.builder()
            .navIdent(ordsRessurs.get("nav_ident").asString())
            .personIdent(ordsRessurs.get("fnr").asString())
            .fornavn(ordsRessurs.get("fornavn").asString())
            .etternavn(ordsRessurs.get("etternavn").asString())
            .remedyAktiv(ordsRessurs.hasNonNull("remedy_aktiv") && ordsRessurs.get("remedy_aktiv").asString().equals("JA"))
            .remedyAnsattType(ordsRessurs.get("ansatt_type").asString())
            .rawData(ordsRessurs)
            .ansattRessursKode(ordsRessurs.get("ansatt_ressurs_kode").asString())
            .ansettelsesTypeBesk(parseNullableFromJson(ordsRessurs, "ansettelsestype_besk"))
            .remedyStartDato(parseNullableDateFromJson(ordsRessurs, "remedy_start_dato"))
            .remedySluttdato(parseNullableDateFromJson(ordsRessurs, "remedy_slutt_dato"))
            .agressoStartDato(parseNullableDateFromJson(ordsRessurs, "agresso_start_dato"))
            .agressoSluttdato(parseNullableDateFromJson(ordsRessurs, "agresso_slutt_dato"))
            .agressoAnsattIDag(ordsRessurs.hasNonNull("ansatt_per_idag") && ordsRessurs.get("ansatt_per_idag").asString().equals("JA"))
            .remedyPermisjon(ordsRessurs.get("remedy_permisjon").asString("NEI").equals("JA"))
            .identType(ordsRessurs.get("ident_type").asString())
            .epost(ordsRessurs.get("epost").asString())
            .fysiskBrukersted(parseNullableFromJson(ordsRessurs, "fysisk_brukersted"))
            .build();

        // orgenhetPaaRessurs = null;
        // genhetPaaRessursBeskrivelse = null;
        // orgenhetPaaRessursNiva = null;
    }

    public static DvhRessurs fromOrdsKommunalRessurs(JsonNode ordsKommunalRessurs) {
        return DvhRessurs.builder()
            .navIdent(ordsKommunalRessurs.get("navid").asString())
            .personIdent(ordsKommunalRessurs.get("fnr ").asString())
            .fornavn(ordsKommunalRessurs.get("navn ").asString())
            .etternavn(ordsKommunalRessurs.get("etternavn ").asString())
            .remedyAktiv(ordsKommunalRessurs.hasNonNull("aktiv") && ordsKommunalRessurs.get("aktiv").asString().equals("JA"))
            .remedyAnsattType(ordsKommunalRessurs.get("ansatt_type").asString())
            .rawData(ordsKommunalRessurs)
            .ansattRessursKode(ordsKommunalRessurs.get("type").asString())
            .ansettelsesTypeBesk(parseNullableFromJson(ordsKommunalRessurs, "ansettelsestype_besk"))
            .remedyStartDato(parseNullableDateFromJson(ordsKommunalRessurs, "start_dato"))
            .remedySluttdato(parseNullableDateFromJson(ordsKommunalRessurs, "slutt_dato"))
            .agressoStartDato(parseNullableDateFromJson(ordsKommunalRessurs, "date_from"))
            .agressoSluttdato(parseNullableDateFromJson(ordsKommunalRessurs, "date_to"))
            .agressoAnsattIDag(ordsKommunalRessurs.hasNonNull("ansatt_idag") && ordsKommunalRessurs.get("ansatt_idag").asString().equals("JA"))
            .remedyPermisjon(ordsKommunalRessurs.get("permisjon").asString("NEI").equals("JA"))
            .identType(ordsKommunalRessurs.get("identtype").asString())
            .epost(ordsKommunalRessurs.get("epost ").asString())
            .fysiskBrukersted(parseNullableFromJson(ordsKommunalRessurs, "fysisk_brukersted"))
            .orgenhetPaaRessurs(parseNullableFromJson(ordsKommunalRessurs, "orgenhet_paa_ressurs"))
            .orgenhetPaaRessursBeskrivelse(parseNullableFromJson(ordsKommunalRessurs, "orgenhet_paa_ressurs_beskrivelse"))
            .orgenhetPaaRessursNiva(parseNullableFromJson(ordsKommunalRessurs, "orgenhet_paa_ressurs_niva"))
            .build();
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


    private boolean withinSixMonths(LocalDate d1, LocalDate d2) {
        var stepLen = 6 / 2;
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

    /*
        Statlig         B	Leder
        Ekstern         E	Eksterne(E)
        Statlig         F	Fast ansatte(F)
        Ekstern         H	Ekstern honorar
        Dropp denne     I	Ikkje relevant
        Ekstern         J	Ekstern jobbintervju
        Kommunal        K	Kommunal leder
        Dropp denne     L	Lese og sekretærhjelp
        Statlig         M	Midlertidig ansatte(M)
        Kommunal        N	Kommunalt ansatt (N)
        Ekstern         O	Ekstern Konsulent
        Statlig         P	Pensjonister
        ?               Q	RPA Robot
        Statlig         R	Lærling
        Statlig/ekstern S	Student
        Statlig/ekstern T	Tolk
        Ekstern         U	Arbeidstrening
        Dropp denne     V	Ventelønn
        Statlig/ekstern Y	Timelønna
     */
    public Sektor getU4Sektor() {
        List<String> statligeSektorer = List.of("B", "F", "M", "P", "R");
        List<String> kommunaleSektorer = List.of("K", "N");
        List<String> eksterneSektorer = List.of("E", "H", "J", "O", "U");
        if (ansattRessursKode == null) {
            return null;
        } else if (statligeSektorer.contains(ansattRessursKode)) {
            return Sektor.NAV_STATLIG;
        } else if (kommunaleSektorer.contains(ansattRessursKode)) {
            return Sektor.NAV_KOMMUNAL;
        } else if (eksterneSektorer.contains(ansattRessursKode)) {
            return Sektor.EKSTERN;
        }
        else return null;
    }

    public boolean isAktivIU4() {
        return agressoStartDato != null && PeriodeUtil.isCurrentOrFuture(agressoSluttdato);
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

    private static LocalDate parseNullableDateFromJson(JsonNode jsonNode, String s) {
        return jsonNode.hasNonNull(s) ? LocalDate.parse(jsonNode.get(s).asString()) : null;
    }

    private static String parseNullableFromJson(JsonNode jsonNode, String s) {
        return jsonNode.hasNonNull(s) ? jsonNode.get(s).asString() : null;
    }
}

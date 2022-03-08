package no.nav.nom.apps.api.orgenhet;

import lombok.experimental.UtilityClass;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static no.nav.nom.libs.utils.common.util.StreamUtils.toMap;

@UtilityClass
public class OrgTyper {

    private final static Map<String, OrgType> typer;
    private final static Map<String, String> agressoToNomType;

    static {
        typer = toMap(List.of(
                OrgType.builder().kode("AA")            .navn("AA-reg")                             .build(),
                OrgType.builder().kode("ALS")           .navn("Arbeidslivssenter")                  .build(),
                OrgType.builder().kode("ARK")           .navn("Arbeidsrådgivningskontor")           .build(),
                OrgType.builder().kode("DIR")           .navn("Direktorat")                         .build(),
                OrgType.builder().kode("DOKSENTER")     .navn("Dokumentsenter")                     .build(),
                OrgType.builder().kode("FP")            .navn("Familie- og pensjonsytelser")        .build(),
                OrgType.builder().kode("FYLKE")         .navn("Fylkeskontor")                       .build(),
                OrgType.builder().kode("HMS_HOT")       .navn("Hjelpemidler og tilrettelegging")    .build(),
                OrgType.builder().kode("KLAGE")         .navn("Klageinstans")                       .build(),
                OrgType.builder().kode("KONTROLL")      .navn("Kontrollenhet")                      .build(),
                OrgType.builder().kode("KS")            .navn("Kontaktsenter")                      .build(),
                OrgType.builder().kode("NAV_KTR")       .navn("Nav-kontor")                         .build(),
                OrgType.builder().kode("NAY")           .navn("Arbeid og ytelser")                  .build(),
                OrgType.builder().kode("OKO")           .navn("Økonomi")                            .build(),
                OrgType.builder().kode("OKO_PEN")       .navn("Økonomi pensjon")                    .build(),
                OrgType.builder().kode("OKO_STONAD")    .navn("Økonomi stønad")                     .build(),
                OrgType.builder().kode("OKO_TJENESTE")  .navn("Økonomitjeneste")                    .build(),
                OrgType.builder().kode("ROL")           .navn("Rådgivende overlege")                .build(),
                OrgType.builder().kode("TILTAK")        .navn("Tiltak")                             .build(),
                OrgType.builder().kode("UTL")           .navn("Oppfølging utland")                  .build()
        ), OrgType::getKode);

        agressoToNomType = Map.ofEntries(
                entry("11", "NAY"),
                entry("21", "FYLKE"),
                entry("30", "DOKSENTER"),
                entry("31", "NAV_KTR"),
                entry("33", "ARK"),
                entry("34", "TILTAK"),
                entry("37", "ALS"),
                entry("40", "ROL"),
                entry("42", "KS"),
                entry("52", "FP"),
                entry("53", "KLAGE"),
                entry("54", "KONTROLL"),
                entry("60", "OKO"),
                entry("80", "DIR"),
                entry("83", "HMS_HOT")
        );
        agressoToNomType.forEach((agressoType, type) -> Assert.isTrue(typer.containsKey(type),
                "Invalid mapping av agresso kode <%s> - <%s> finnes ikke".formatted(agressoType, type)));
    }

    public static OrgType getOrgType(String type) {
        return type == null ? null : typer.get(type);
    }

    public static OrgType forAgressoOrgType(String agressoType) {
        if (agressoType == null || !agressoToNomType.containsKey(agressoType)) {
            return null;
        }
        String kode = agressoToNomType.get(agressoType);
        return kode == null ? null : getOrgType(kode);
    }
}

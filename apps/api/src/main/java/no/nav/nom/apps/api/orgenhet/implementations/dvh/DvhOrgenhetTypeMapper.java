package no.nav.nom.apps.api.orgenhet.implementations.dvh;

import lombok.val;
import no.nav.nom.apps.api.orgenhet.contract.OrgenhetType;

import java.util.Map;

import static java.util.Map.entry;

public class DvhOrgenhetTypeMapper {
    public static OrgenhetType agressoToNomType(String agressoOrgType){
        val agressoToNomType = Map.ofEntries(
                entry("11", OrgenhetType.NAY),
                entry("21", OrgenhetType.FYLKE),
                entry("30", OrgenhetType.DOKSENTER),
                entry("31", OrgenhetType.NAV_KTR),
                entry("33", OrgenhetType.ARK),
                entry("34", OrgenhetType.TILTAK),
                entry("37", OrgenhetType.ALS),
                entry("40", OrgenhetType.ROL),
                entry("42", OrgenhetType.KS),
                entry("52", OrgenhetType.FP),
                entry("53", OrgenhetType.KLAGE),
                entry("54", OrgenhetType.KONTROLL),
                entry("60", OrgenhetType.OKO),
                entry("80", OrgenhetType.DIR),
                entry("83", OrgenhetType.HMS_HOT)
        );

        return agressoOrgType != null ? agressoToNomType.getOrDefault(agressoOrgType, null) : null;
    }
}

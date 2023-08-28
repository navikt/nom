package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.util.Optional;

public class NomNivaaMapper {

    public static Optional<NomNivaa> resolve(OrgEnhetsType orgEnhetsType, String agressoOrgNivaa) {
        if (OrgEnhetsType.DIREKTORAT.equals(orgEnhetsType)) {
            return switch (agressoOrgNivaa) {
                case "ORGNIV1" -> Optional.of(NomNivaa.DRIFTSENHET);
                case "ORGNIV2" -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        } else {
            return switch (agressoOrgNivaa) {
                case "ORGNIV1" -> Optional.of(NomNivaa.LINJEENHET);
                case "ORGNIV2" -> Optional.of(NomNivaa.DRIFTSENHET);
                case "ORGNIV3" -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        }
    }
}

package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.util.Optional;

public class NomNivaaMapper {

    public static Optional<NomNivaa> resolve(OrgEnhetsType type, String orgniv) {
        if (OrgEnhetsType.DIR.equals(type)) {
            return switch (orgniv) {
                case "ORGNIV1" -> Optional.of(NomNivaa.DRIFTSENHET);
                case "ORGNIV2" -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        } else {
            return switch (orgniv) {
                case "ORGNIV1" -> Optional.of(NomNivaa.LINJEENHET);
                case "ORGNIV2" -> Optional.of(NomNivaa.DRIFTSENHET);
                case "ORGNIV3" -> Optional.of(NomNivaa.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        }
    }
}

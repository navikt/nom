package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.contract.nom.OrgEnhetsKategori;
import no.nav.nom.contract.nom.OrgEnhetsType;

import java.util.Optional;

public class NomNivaaMapper {

    public static Optional<OrgEnhetsKategori> resolve(OrgEnhetsType type, String orgniv) {
        if (OrgEnhetsType.DIR.equals(type)) {
            return switch (orgniv) {
                case "ORGNIV1" -> Optional.of(OrgEnhetsKategori.DRIFTSENHET);
                case "ORGNIV2" -> Optional.of(OrgEnhetsKategori.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        } else {
            return switch (orgniv) {
                case "ORGNIV1" -> Optional.of(OrgEnhetsKategori.LINJEENHET);
                case "ORGNIV2" -> Optional.of(OrgEnhetsKategori.DRIFTSENHET);
                case "ORGNIV3" -> Optional.of(OrgEnhetsKategori.ARBEIDSOMRAADE);
                default -> Optional.empty();
            };
        }
    }
}

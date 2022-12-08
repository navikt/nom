package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;


import no.nav.nom.contract.nom.OrgEnhetsKategori;

public class StaticOrgenhetkategoriMapping {
    public static OrgEnhetsKategori resolveFor(String agressoId, String orgniv) {
        return switch (orgniv) {
            case "ORGNIV1" -> {
                if ("1".equals(agressoId) || "4".equals(agressoId)) {
                    yield OrgEnhetsKategori.LINJEENHET;
                } else if ("80".equals(agressoId)) {
                    yield OrgEnhetsKategori.DRIFTSENHET;
                } else {
                    yield null;
                }
            }
            case "ORGNIV2" -> {
                if ("821".equals(agressoId)) {
                    yield OrgEnhetsKategori.ARBEIDSOMRAADE;
                } else if ("48".equals(agressoId) || "R10".equals(agressoId)) {
                    yield OrgEnhetsKategori.DRIFTSENHET;
                } else {
                    yield null;
                }
            }
            case "ORGNIV3" -> {
                if ("480600".equals(agressoId) || "090600".equals(agressoId)) {
                    yield OrgEnhetsKategori.ARBEIDSOMRAADE;
                } else {
                    yield null;
                }
            }
            default -> null;
        };
    }
}

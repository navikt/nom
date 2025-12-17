package no.nav.nom.apps.api.teamcat;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public record TeamcatRessursDTO(
    LocalDate startdato,
    LocalDate sluttdato,
    String epost,
    String navident,
    ResourceType ressurstype,
    String visningsnavn,
    String enhetId,
    String lederNavident
) {
    public enum ResourceType {
        EKSTERN("EKSTERN"),
        INTERN("INTERN");

        public final String textRepresentation;

        ResourceType(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static ResourceType from(Sektor sektor) {
            if (sektor.equals(Sektor.EKSTERN)) {
                return ResourceType.EKSTERN;
            }
            return ResourceType.INTERN;
        }
    }
}

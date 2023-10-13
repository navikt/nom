package no.nav.nom.apps.api.ressurs.api.v1.teamcat;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

public record TeamcatRessursDTO(
    LocalDate startdato,
    LocalDate sluttdato,
    String epost,
    String navident,
    ResourceType ressurstype,
    String visningsnavn,

    String avdelingnavn
) {
    public enum ResourceType {
        ANNEN_STAT("ANNEN_STAT"),
        EKSTERN("EKSTERN"),
        INTERN("INTERN");


        public final String textRepresentation;


        ResourceType(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static ResourceType from(Sektor sektor, boolean isAnnenStat) {
            if (isAnnenStat) {
                return ResourceType.ANNEN_STAT;
            }
            if (sektor.equals(Sektor.EKSTERN)) {
                return ResourceType.EKSTERN;
            }
            return ResourceType.INTERN;
        }
    }
}

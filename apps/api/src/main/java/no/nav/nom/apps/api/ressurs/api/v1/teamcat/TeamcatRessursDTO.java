package no.nav.nom.apps.api.ressurs.api.v1.teamcat;

import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;
import java.util.List;

public record TeamcatRessursDTO(
    LocalDate startdato,
    LocalDate sluttdato,
    String epost,
    String navident,
    ResourceType ressurstype,
    String visningsnavn

) {

    /*
            switch (ressursType) {
            case "INTERN":
                return INTERNAL;
            case "EKSTERN":
                return EXTERNAL;
            case "ANNEN_STAT":
                // Annen statlig org? Helfo etc kommer her, filtrerer de ut
                return OTHER;
            default:
                log.warn("unknown resource type {}", ressursType);
                return OTHER;
        }
     */

    public enum ResourceType {
        ANNEN_STAT("ANNEN_STAT"),
        EKSTERN("EKSTERN"),
        INTERN("INTERN");


        public final String textRepresentation;


        ResourceType(String textRepresentation) {
            this.textRepresentation = textRepresentation;
        }

        public static ResourceType fromSektor(Sektor sektor, boolean isAnnenStat) {
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

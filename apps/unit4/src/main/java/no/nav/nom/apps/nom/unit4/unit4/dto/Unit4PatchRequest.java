package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.nom.apps.nom.unit4.unit4.Utils;

import java.time.LocalDate;
import java.util.List;

public record Unit4PatchRequest(
        Operation op,
        String path, // Se apps/unit4/unit4PatchResponse.json for paths
        Object value
) {


    // Husk at Unit4 ønsker folkeregistrert navn formatert med apps/unit4/src/main/java/no/nav/nom/apps/nom/unit4/unit4/Utils.java
    // sin formatName metode
    public static List<Unit4PatchRequest> createEndreNavnPatchRequest(String fornavn, String etternavn) {
        return List.of(
                new Unit4PatchRequest(Operation.REPLACE, "nameInformation.firstName", Utils.formatName(fornavn)),
                new Unit4PatchRequest(Operation.REPLACE, "nameInformation.surname", Utils.formatName(etternavn))
        );
    }

    // Må avklares!!
    // Skal orgtilknytning på employment (stilling) relatedValues endres?
    public static List<Unit4PatchRequest> createEndreOrgtilknytningPatchRequest(String agressoId) {
        return List.of(
                new Unit4PatchRequest(Operation.ADD_OR_REPLACE_BY_ID,
                        "relatedValues",
                        RelatedValue.builder()
                                .relationId("M0")
                                .relatedValue(agressoId)
                                .dateFrom(LocalDate.now().atStartOfDay())
                                .dateTo(Unit4RessursDto.AGRESSO_TIMESTAMP_NULL_VALUE)
                                .build()
                )
        );
    }

    // Må avklares!!
    // Skal ressurstype på employment (stilling) endres?
    public static List<Unit4PatchRequest> createEndreRessurstypePatchRequest(Unit4RessursType ressursType) {
        return List.of(
                new Unit4PatchRequest(Operation.REPLACE, "personnelType", ressursType)
        );
    }


    // Må avklares!!
    // Hva skal status på ressurs settes til hvis det er frem i tid eller det er noen som allrede har sluttet?
    // status og dateTo på relatedValues?
    // status og dateTo på employment og dens relatedValues?
    // andre ting som må endres i unit4 når en ressurs endrer startdato?
    public static List<Unit4PatchRequest> createEndreStartDatoPatchRequest(LocalDate startDato) {
        return List.of(
                new Unit4PatchRequest(Operation.REPLACE, "dateFrom", startDato.atStartOfDay())
        );
    }

    // Må avklares!!
    // Hva skal status på ressurs settes til?
    // status og dateTo på relatedValues?
    // status og dateTo på employment og dens relatedValues?
    // andre ting som må endres i unit4 når en ressurs slutter?
    public static List<Unit4PatchRequest> createEndreSluttDatoPatchRequest(LocalDate sluttDato) {
        return List.of(
                new Unit4PatchRequest(Operation.REPLACE, "dateTo", sluttDato.atStartOfDay())
                //new Unit4PatchRequest(Operation.REPLACE, "status", "-")
        );
    }

    enum Operation {
        ADD("Add"),
        REPLACE("Replace"),
        REMOVE("Remove"),
        ADD_OR_REPLACE_BY_ID("AddOrReplaceById"),
        REPLACE_BY_ID("ReplaceById"),
        REMOVE_BY_ID("RemoveById");

        private final String value;

        Operation(String value) {
            this.value = value;
        }

        @JsonValue // Jackson will serialize this enum as the value of the field
        public String getValue() {
            return value;
        }

    }
}

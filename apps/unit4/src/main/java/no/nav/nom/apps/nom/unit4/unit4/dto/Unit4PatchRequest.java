package no.nav.nom.apps.nom.unit4.unit4.dto;

public record Unit4PatchRequest(
        Operation op,
        String path,
        String value
) {


    enum Operation {
        REPLACE,
        REMOVE,
        AddOrReplaceByID,

    }
}

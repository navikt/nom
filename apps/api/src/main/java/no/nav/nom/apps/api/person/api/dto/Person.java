package no.nav.nom.apps.api.person.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Person(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String personident,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) String navident,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FolkeregisterPerson folkeregisterPerson
) { }



package no.nav.nom.apps.api.ressurs.api.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;

import java.time.LocalDate;

@Data
@Builder
public class OpprettRessursRequest {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String personident;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String visningsfornavn;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String visningsmellomnavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String visningsetternavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Sektor sektor;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String orgEnhetId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDato;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate sluttDato;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private AnsettelsesType ansettelsesType;
}

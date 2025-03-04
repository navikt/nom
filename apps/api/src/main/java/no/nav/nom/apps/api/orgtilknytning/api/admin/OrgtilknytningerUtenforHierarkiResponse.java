package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;

@Data
@Builder
public class OrgtilknytningerUtenforHierarkiResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String navident;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String visningsnavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Sektor ressursSektor;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String orgtilknytningFid;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetNavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetRemedyEnhet;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate orgtilknytningGyldigFom;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate orgtilknytningGyldigTom;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Sektor orgtilknytningSektor;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Tilknytningtype orgtilknytningType;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alternativOrgenhetId;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alternativOrgenhetNavn;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alternativOrgenhetRemedyEnhet;

    public static OrgtilknytningerUtenforHierarkiResponse from(
        RessursState ressurs, OrgtilknytningState orgtilknytning, IOrgenhet otOrgEnhet, String otRemedyEnhet,
        IOrgenhet alternativOrgEnhet, String alternativRemedyEnhet)
    {
        return OrgtilknytningerUtenforHierarkiResponse.builder()
                .navident(ressurs.navident())
                .visningsnavn(ressurs.visningsnavn().getFormattedVisningsnavn())
                .ressursSektor(ressurs.sektor())
                .orgtilknytningFid(orgtilknytning.fid())
                .faktiskOrgenhetId(otOrgEnhet.getId())
                .faktiskOrgenhetNavn(otOrgEnhet.getNavn())
                .faktiskOrgenhetRemedyEnhet(otRemedyEnhet)
                .orgtilknytningGyldigFom(orgtilknytning.gyldigFom())
                .orgtilknytningGyldigTom(orgtilknytning.gyldigTom().orElse(null))
                .orgtilknytningSektor(orgtilknytning.sektor())
                .orgtilknytningType(orgtilknytning.orgtilknytningType().orElse(null))
                .alternativOrgenhetId(alternativOrgEnhet.getId())
                .alternativOrgenhetNavn(alternativOrgEnhet.getNavn())
                .alternativOrgenhetRemedyEnhet(alternativRemedyEnhet)
                .build();
    }
}

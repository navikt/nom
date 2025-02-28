package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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
    private String orgtilknytingGydligFid;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetNavn;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String faktiskOrgenhetRemedyEnhet;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate orgtilknytingGydligFom;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate orgtilknytingGydligTom;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Sektor orgtilknytingSektor;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Tilknytningtype orgtilknytingsType;

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
                .orgtilknytingGydligFid(orgtilknytning.fid())
                .faktiskOrgenhetId(otOrgEnhet.getId())
                .faktiskOrgenhetNavn(otOrgEnhet.getNavn())
                .faktiskOrgenhetRemedyEnhet(otRemedyEnhet)
                .orgtilknytingGydligFom(orgtilknytning.gyldigFom())
                .orgtilknytingGydligTom(orgtilknytning.gyldigTom().orElse(null))
                .orgtilknytingSektor(orgtilknytning.sektor())
                .orgtilknytingsType(orgtilknytning.orgtilknytningType().orElse(null))
                .alternativOrgenhetId(alternativOrgEnhet.getId())
                .alternativOrgenhetNavn(alternativOrgEnhet.getNavn())
                .alternativOrgenhetRemedyEnhet(alternativRemedyEnhet)
                .build();
    }
}

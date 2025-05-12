package no.nav.nom.apps.admin.sanitycheck.dto.orgenhet;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record OrgEnhetArbeidsomraadeInfo(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int antallLoevnoder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<LoevnodeMedFeilAntallArbOmr> loevnoderMedFeil
) {
    
    @Builder
    public record LoevnodeMedFeilAntallArbOmr(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orgenhetNavn,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int antallArbOmr
    ) {}
}



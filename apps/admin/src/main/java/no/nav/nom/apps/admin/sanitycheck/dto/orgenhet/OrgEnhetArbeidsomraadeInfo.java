package no.nav.nom.apps.admin.sanitycheck.dto.orgenhet;

import lombok.Builder;

import java.util.List;

@Builder
public record OrgEnhetArbeidsomraadeInfo(
        int antallLoevnoder,
        List<LoevnodeMedFeilAntallArbOmr> loevnoderMedFeil
) {
    
    @Builder
    public record LoevnodeMedFeilAntallArbOmr(
            String orgenhetId,
            String orgenhetNavn,
            int antallArbOmr
    ) {}
}



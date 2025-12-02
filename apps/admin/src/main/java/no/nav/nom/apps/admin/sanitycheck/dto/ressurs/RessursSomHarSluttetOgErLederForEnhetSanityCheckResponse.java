package no.nav.nom.apps.admin.sanitycheck.dto.ressurs;

import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse(
    String navident,
    String visningsnavn,
    LocalDate sluttdato,
    List<EnhetSanityCheckResponse> enheter
) {
    public record EnhetSanityCheckResponse(
        String navn,
        String id,
        String agressoId
    ) {}

    public static List<RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse> from(Map<RessursState, List<OrgEnhetState>> ressursEnhetMap) {
        return ressursEnhetMap.entrySet()
            .stream()
            .map(ressursStateListEntry -> from(ressursStateListEntry.getKey(), ressursStateListEntry.getValue()))
            .toList();
    }

    public static RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse from(RessursState ressursState, List<OrgEnhetState> orgenheter) {
        return new RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse(
            ressursState.navident(),
            ressursState.visningsnavn().getFormattedVisningsnavn(),
            ressursState.sluttdato(),
            orgenheter.stream().map(orgenhet -> new EnhetSanityCheckResponse(orgenhet.navn(), orgenhet.id(), orgenhet.uniqueAgressoId())).toList()
        );
    }
}

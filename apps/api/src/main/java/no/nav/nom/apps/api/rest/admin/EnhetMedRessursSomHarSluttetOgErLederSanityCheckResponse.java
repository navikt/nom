package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse(
    String navident,
    String visningsnavn,
    LocalDate sluttdato,
    List<EnhetsOrganisasjonSanityCheckResponse> enhetsOrganisasjonSanityCheckResponse
) {
    record EnhetsOrganisasjonSanityCheckResponse(
        String navn,
        String id,
        String agressoId
    ) {}

    public static List<EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse> from(Map<RessursState, List<IOrgenhet>> ressursEnhetMap) {
        return ressursEnhetMap.entrySet().stream().map(ressursStateListEntry -> from(ressursStateListEntry.getKey(), ressursStateListEntry.getValue())).toList();
    }

    public static EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse from(RessursState ressursState, List<IOrgenhet> orgenheter) {
        return new EnhetMedRessursSomHarSluttetOgErLederSanityCheckResponse(
            ressursState.navident(),
            ressursState.visningsnavn().getFormattedVisningsnavn(),
            ressursState.sluttdato(),
            orgenheter.stream().map(orgenhet -> new EnhetsOrganisasjonSanityCheckResponse(orgenhet.getNavn(), orgenhet.getId(), orgenhet.getAgressoId())).toList());
    }
}

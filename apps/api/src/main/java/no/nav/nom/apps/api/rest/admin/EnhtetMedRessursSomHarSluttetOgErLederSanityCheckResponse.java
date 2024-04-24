package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse(
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

    public static List<EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse> from(Map<RessursState, List<IOrgenhet>> ressursEnhetMap) {
        return ressursEnhetMap.entrySet().stream().map(ressursStateListEntry -> from(ressursStateListEntry.getKey(), ressursStateListEntry.getValue())).toList();
    }

    public static EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse from(RessursState ressursState, List<IOrgenhet> orgenheter) {
        return new EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse(
            ressursState.navident(),
            ressursState.visningsnavn().getFormattedVisningsnavn(),
            ressursState.sluttdato().orElse(null),
            orgenheter.stream().map(orgenhet -> new EnhetsOrganisasjonSanityCheckResponse(orgenhet.getNavn(), orgenhet.getId(), orgenhet.getAgressoId())).toList());
    }
}

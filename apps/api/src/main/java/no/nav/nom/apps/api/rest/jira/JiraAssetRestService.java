package no.nav.nom.apps.api.rest.jira;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.FysiskBrukerstedNotFoundException;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.RessursService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JiraAssetRestService {

    private final FysiskBrukerstedService fysiskBrukerstedService;
    private final RessursService ressursService;


    public List<JiraAssetResponse> getFysiskBrukerStedWithRessurser() {
        var jiraAsset = new ArrayList<JiraAssetResponse>();

        ressursService.findAllGroupedByFysiskBrukersted().forEach((fysiskBrukerstedNr, ressurser) -> {
            var fysiskBrukersted = fysiskBrukerstedService.findFysiskBrukersted(fysiskBrukerstedNr).orElseThrow(() -> FysiskBrukerstedNotFoundException.notFound(fysiskBrukerstedNr));
            jiraAsset.add(
                new JiraAssetResponse(
                    fysiskBrukersted.nr(),
                    fysiskBrukersted.navn(),
                    fysiskBrukersted.address(),
                    fysiskBrukersted.postnummer(),
                    fysiskBrukersted.poststed(),
                    fysiskBrukersted.kommune(),
                    ressurser.size()
                )
            );
        });
        return jiraAsset;
    }
}

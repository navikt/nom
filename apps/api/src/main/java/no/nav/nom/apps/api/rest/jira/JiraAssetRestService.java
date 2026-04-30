package no.nav.nom.apps.api.rest.jira;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.RessursService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraAssetRestService {

    private final FysiskBrukerstedService fysiskBrukerstedService;
    private final RessursService ressursService;


    public List<JiraAssetResponse> getFysiskBrukerStedWithRessurser() {
        var jiraAsset = new ArrayList<JiraAssetResponse>();
        var fysiskeBrukersteder = fysiskBrukerstedService.getAllFysiskBrukersted();
        log.info("Alle fysiskeBrukersteder : {}", fysiskeBrukersteder);

        ressursService.findAllGroupedByFysiskBrukersted().forEach((fysiskBrukerstedNr, ressurser) -> {
            var fysiskBrukersted = fysiskeBrukersteder.get(fysiskBrukerstedNr);
            if (isNull(fysiskBrukersted)) {
                log.info("Dette fysiskeBrukerstedet {} finnes ikke??", fysiskBrukerstedNr);
            }
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

package no.nav.nom.apps.api.graphql.search;

import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
public class SearchResolver {

    private final UnleashRessursServiceSelector ressursService;
    private final IOrgenhetService orgenhetService;

    public SearchResolver(UnleashRessursServiceSelector ressursService, IOrgenhetService orgenhetService) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
    }

    public List<Object> search(String term) {
        var matchedOrgenhetIds = Stream.concat(
                        orgenhetService.matchByAgressoId(term).stream(),
                        orgenhetService.matchByNavn(term).stream()
                )
                .map(IOrgenhet::getAgressoId)
                .collect(toSet());

        return Stream.concat(
                        orgenhetService.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(OrgEnhetResolver::convertToOrgEnhet),
                        ressursService.matchByNavident(term).stream().map(MappingUtils::toRessurs)
                )
                .toList();


    }
}

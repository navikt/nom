package no.nav.nom.apps.api.graphql.search;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
public class SearchResolver {

    private final UnleashRessursServiceSelector ressursService;
    private final IOrgenhetService orgenhetService;
    private final LuceneService luceneService;

    public static final String UNLEASH_LUCENE_TOGGLE = "nom.ressurs.lucene_search_toggle";

    private Unleash unleash;

    public SearchResolver(UnleashRessursServiceSelector ressursService, IOrgenhetService orgenhetService, LuceneService luceneService, Unleash unleash) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.luceneService = luceneService;
        this.unleash = unleash;
    }

    public List<Object> search(String term) {
        var matchedOrgenhetIds = Stream.concat(
                        orgenhetService.matchByAgressoId(term).stream(),
                        orgenhetService.matchByNavn(term).stream()
                )
                .map(IOrgenhet::getAgressoId)
                .collect(toSet());

        var orgenheter = orgenhetService.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(OrgEnhetResolver::convertToOrgEnhet);

        Stream<Object> ressurser;

        if(unleash.isEnabled(UNLEASH_LUCENE_TOGGLE)){
            var matchedNavidentsSorted = luceneService.search(term).stream().map(SearchResult::id).toList();
            var matchedNavidentsMap = ressursService.findByNavidentBulk(matchedNavidentsSorted);
            ressurser = matchedNavidentsSorted.stream().map(r -> MappingUtils.toRessurs(matchedNavidentsMap.get(r)));
        }else{
            ressurser = ressursService.matchByNavident(term).stream().map(MappingUtils::toRessurs);
        }

        return Stream.concat(orgenheter, ressurser).toList();

    }
}

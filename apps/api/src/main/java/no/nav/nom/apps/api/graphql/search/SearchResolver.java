package no.nav.nom.apps.api.graphql.search;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Component
@Import(no.nav.nom.libs.utils.common.unleash.Config.class)
public class SearchResolver {

    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;
    private final LuceneService luceneService;

    public static final String UNLEASH_LUCENE_TOGGLE = "nom.ressurs.lucene_search_toggle";

    private final Unleash unleash;

    public SearchResolver(IRessursService ressursService, IOrgenhetService orgenhetService, LuceneService luceneService, Unleash unleash) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.luceneService = luceneService;
        this.unleash = unleash;
    }

    public List<Object> search(String term_inp) {
        Stream<OrgEnhet> orgenheter;
        Stream<Object> ressurser;

        if (unleash.isEnabled(UNLEASH_LUCENE_TOGGLE)) {
            // termer ser ut til å fungere bedre når de er inneklemt med single-quotes
            // men dette skjer kun i API/miljø. Rart at lokalt GUI skal oppføre seg annerledes.
            var matchedNavidentsSorted = luceneService.search(term_inp).stream()
                    .filter(luceneService::filterRessursResult)
                    .map(SearchResult::id).toList();
            var matchedNavidentsMap = ressursService.findByNavidentBulk(matchedNavidentsSorted);
            ressurser = matchedNavidentsSorted.stream().map(r -> MappingUtils.toRessurs(matchedNavidentsMap.get(r)));

            var matchedFidsSorted = luceneService.search(term_inp).stream()
                    .filter(luceneService::filterOrgenhetResult)
                    .map(SearchResult::id).toList();
            Map<String, IOrgenhet> matchedFidsMap = orgenhetService.getByFidBulk(matchedFidsSorted);
            orgenheter = matchedFidsSorted.stream().map(ioe -> OrgEnhetResolver.convertToOrgEnhet(matchedFidsMap.get(ioe)));
        } else {
            var matchedOrgenhetIds = Stream.concat(
                            orgenhetService.matchByAgressoId(term_inp).stream(),
                            orgenhetService.matchByNavn(term_inp).stream()
                    )
                    .map(IOrgenhet::getAgressoId)
                    .collect(toSet());

            orgenheter = orgenhetService.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(OrgEnhetResolver::convertToOrgEnhet);

            ressurser = ressursService.matchByNavident(term_inp).stream().map(MappingUtils::toRessurs);
        }

        return Stream.concat(orgenheter, ressurser).toList();

    }


    public List<Ressurs> searchRessurs(String term) {
        var matchedNavidentsSorted = luceneService.searchRessurs(term).stream();
        var matchedNavidentsMap = ressursService.findByNavidentBulk(matchedNavidentsSorted.map(SearchResult::id).toList());

        return matchedNavidentsSorted
                .map(it -> MappingUtils.toRessurs(matchedNavidentsMap.get(it.id())))
                .toList();
    }



}

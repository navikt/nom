package no.nav.nom.apps.api.graphql.search;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.graphql.common.projections.OrgEnhetSearchFilter;
import no.nav.nom.apps.api.graphql.common.projections.RessursSearchFilter;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class SearchResolver {

    private final IRessursService ressursService;
    private final OrgenhetService orgenhetService;
    private final LuceneService luceneService;

    public static final String UNLEASH_LUCENE_TOGGLE = "nom.ressurs.lucene_search_toggle";

    private final Unleash unleash;

    public SearchResolver(IRessursService ressursService,
                          OrgenhetService orgenhetService,
                          LuceneService luceneService, Unleash unleash) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.luceneService = luceneService;
        this.unleash = unleash;
    }

    public List<Object> search(String term_inp, RessursSearchFilter ressursSearchFilter, OrgEnhetSearchFilter orgEnhetSearchFilter) {
        return Stream.concat(
                luceneService.searchRessurs(term_inp, ressursSearchFilter.toLuceneFilter(), ressursSearchFilter.getLimit().getValue()).stream(),
                luceneService.searchOrgenhet(term_inp, orgEnhetSearchFilter.toLuceneFilter(), orgEnhetSearchFilter.getLimit().getValue()).stream()
            )
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .map(result -> {
                if(result.entityClass().equals(LuceneOrgEnhet.class)){
                    return MappingUtils.toOrgEnhet(orgenhetService.getById(result.id()).orElseThrow());
                } else if(result.entityClass().equals(LuceneRessurs.class)){
                    return MappingUtils.toRessurs(ressursService.findByNavident(result.id()).orElseThrow());
                } else {
                    throw new RuntimeException("Unknown entity class: " + result.entityClass());
                }
            })
            .toList();




        /*Stream<OrgEnhet> orgenheter;
        Stream<Object> ressurser;

        if (unleash.isEnabled(UNLEASH_LUCENE_TOGGLE)) {
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

        return Stream.concat(orgenheter, ressurser).toList();*/

    }


    public List<Ressurs> searchRessurs(String term, RessursSearchFilter filter) {
        var matchedNavidentsSorted = luceneService.searchRessurs(term, filter.toLuceneFilter(), filter.getLimit().getValue());
        var matchedNavidentsMap = ressursService.findByNavidentBulk(matchedNavidentsSorted.stream().map(SearchResult::id).toList());

        return matchedNavidentsSorted.stream()
            .map(it -> MappingUtils.toRessurs(matchedNavidentsMap.get(it.id())))
            .toList();
    }


    public List<OrgEnhet> searchOrgenhet(String term, OrgEnhetSearchFilter filter) {
        try {
            return luceneService.searchOrgenhet(term, filter.toLuceneFilter(), filter.getLimit().getValue()).stream()
                    .map(result -> orgenhetService.getById(result.id()))
                    .map(Optional::get)
                    .map(MappingUtils::toOrgEnhet)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

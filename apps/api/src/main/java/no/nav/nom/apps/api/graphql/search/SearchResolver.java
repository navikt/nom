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

    public SearchResolver(IRessursService ressursService,
                          OrgenhetService orgenhetService,
                          LuceneService luceneService, Unleash unleash) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.luceneService = luceneService;
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

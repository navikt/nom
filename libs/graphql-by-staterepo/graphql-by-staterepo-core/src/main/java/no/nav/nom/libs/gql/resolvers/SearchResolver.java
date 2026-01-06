package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.libs.gql.MappingUtils;
import no.nav.nom.libs.gql.lucenesearch.LuceneService;
import no.nav.nom.libs.gql.projections.OrgEnhetSearchFilter;
import no.nav.nom.libs.gql.projections.RessursSearchFilter;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class SearchResolver {

    private final LuceneService luceneService;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository  orgEnhetStateRepository;


    public SearchResolver(LuceneService luceneService, RessursStateRepository ressursStateRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.luceneService = luceneService;
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public List<Object> search(String term, RessursSearchFilter ressursSearchFilter, OrgEnhetSearchFilter orgEnhetSearchFilter) {
        return Stream.concat(
                luceneService.searchRessurs(term, ressursSearchFilter.toLuceneFilter(), ressursSearchFilter.getLimit().getValue()).stream(),
                luceneService.searchOrgenhet(term, orgEnhetSearchFilter.toLuceneFilter(), orgEnhetSearchFilter.getLimit().getValue()).stream()
            )
            .sorted(Comparator.comparing(SearchResult::score).reversed())
            .map(result -> {
                if (result.entityClass().equals(LuceneOrgEnhet.class)) {
                    return MappingUtils.toOrgEnhet(orgEnhetStateRepository.findById(result.id()).orElseThrow());
                } else if (result.entityClass().equals(LuceneRessurs.class)) {
                    return MappingUtils.toRessurs(ressursStateRepository.findByNavident(result.id()).orElseThrow());
                } else {
                    throw new RuntimeException("Unknown entity class: " + result.entityClass());
                }
            })
            .toList();
    }

    public List<Ressurs> searchRessurs(String term, RessursSearchFilter filter) {
        var matchedNavidentsSorted = luceneService.searchRessurs(term, filter.toLuceneFilter(), filter.getLimit().getValue());
        var matchedNavidentsMap = ressursStateRepository.findByNavidentBulk(matchedNavidentsSorted.stream().map(SearchResult::id).toList());

        return matchedNavidentsSorted.stream()
            .map(it -> MappingUtils.toRessurs(matchedNavidentsMap.get(it.id())))
            .toList();
    }

    public List<OrgEnhet> searchOrgenhet(String term, OrgEnhetSearchFilter filter) {
        try {
            return luceneService.searchOrgenhet(term, filter.toLuceneFilter(), filter.getLimit().getValue()).stream()
                .map(result -> orgEnhetStateRepository.findById(result.id()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MappingUtils::toOrgEnhet)
                .toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

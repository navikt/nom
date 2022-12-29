package no.nav.nom.libs.lucenewrapper;

import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

class Helpers {

    static WrappedLucene initScenario(List<Object> entities) {
        var indexRessurs = new RessursIndexer();
        var indexOrgEnhet = new OrgEnhetIndexer();
        var searchRepo = new WrappedLucene(List.of(indexRessurs,indexOrgEnhet));
        for (var o : entities) {
            if (o instanceof LuceneRessurs) {
                searchRepo.upsertEntity((LuceneRessurs) o, indexRessurs);
            } else if (o instanceof LuceneOrgEnhet) {
                searchRepo.upsertEntity((LuceneOrgEnhet) o, indexOrgEnhet);
            } else {
                throw new IllegalStateException("Entity must be Ressurs or OrgEnhet. Got: " + o.getClass().getName());
            }
        }
        return searchRepo;
    }

    static void assertRetrievedIds(List<ExpectedSearchResult> expected, List<SearchResult> actuals) {
        var expectedList = new ArrayList<>(expected);
        var count = actuals.size();
        for (var result : actuals) {
            var removed = expectedList.removeIf(expectedItem -> expectedItem.equalsTo(result));
            if (removed) {
                count -= 1;
            }
            Assertions.assertTrue(removed);
        }
        Assertions.assertEquals(0, expectedList.size());
        Assertions.assertEquals(0, count);
    }

    static void assertRetrievedIds(List<String> ids, String commonTypeStr, List<SearchResult> actual) {
        var expectedSearchResultList = ids.stream()
            .map(it -> new ExpectedSearchResult(it, commonTypeStr))
            .toList();
        assertRetrievedIds(expectedSearchResultList, actual);
    }

    static void assertBestMatch(ExpectedSearchResult expected, List<SearchResult> actual) {
        Assertions.assertTrue(actual.size() > 0, "At least one match");
        var best = actual.get(0);
        Assertions.assertTrue(expected.equalsTo(best), () -> "Best match [%s] did not match expected result [%s]".formatted(best, expected));
    }

    record ExpectedSearchResult(String id, String entityType) {
        public boolean equalsTo(SearchResult result) {
            return id.equals(result.id()) && entityType().equals(result.entityType());
        }
    }

    static ExpectedSearchResult fromRessurs(LuceneRessurs r, String typestr) {
        return new ExpectedSearchResult(r.navident(), typestr);
    }

    static ExpectedSearchResult fromOrgenhet(LuceneOrgEnhet r, String typestr) {
        return new ExpectedSearchResult(r.id(), typestr);
    }
}

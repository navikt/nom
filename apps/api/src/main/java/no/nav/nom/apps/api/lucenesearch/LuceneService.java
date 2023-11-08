package no.nav.nom.apps.api.lucenesearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessursFilter;
import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.search.QueryFilter;
import no.nav.nom.libs.lucenewrapper.search.QueryFilterContext;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class LuceneService {

    private final WrappedLucene wrappedLucene;

    private final RessursStateRepository ressursStateRepository;

    private List<String> invalidatedNavidentList = new ArrayList<>();
    private List<String> invalidatedOrgEnhetList = new ArrayList<>();

    private final OrgenhetService orgenhetService;

    private final String orgenhetEntityType;
    private final String ressursEntityType;

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);

    private final boolean isLocal;

    public LuceneService(RessursStateRepository ressursStateRepository, OrgenhetService orgenhetService, Environment environment) {
        this.orgenhetService = orgenhetService;
        var indexOrgEnhet = new OrgEnhetIndexer();
        var indexRessurs = new RessursIndexer();
        this.wrappedLucene = new WrappedLucene(List.of(indexOrgEnhet, indexRessurs));

        this.orgenhetEntityType = indexOrgEnhet.typeString();
        this.ressursEntityType = indexRessurs.typeString();

        this.ressursStateRepository = ressursStateRepository;
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        isLocal = profiles.contains("local") || profiles.contains("test");
    }

    @Scheduled(initialDelayString = "PT2M", fixedRateString = "PT30S")
    public void addInvalidatedEntitiesToIndex() {
        var ressurserCountToIndex = this.invalidatedNavidentList.size();
        var orgenheterCountToIndex = this.invalidatedOrgEnhetList.size();
        var sumCount = ressurserCountToIndex + orgenheterCountToIndex;
        if (sumCount == 0) {
            return;
        }
        log.info("Lucene indexing started. Number of entities:" + sumCount);

        // ressurser
        {
            final var navidentsToIndex =  grabAndResetNavidentInvalidationList();
            final var luceneRessursStream = navidentsToIndex.stream()
                    .map(ressursStateRepository::findByNavident)
                    .map(Optional::get)
                    // Indexer kun ressurser til søk kun for de med vanlig personnummer - Fjerner type 'Nav Ukjent' ressurser slik som https://nom.nav.no/ressurs/U165173
                    .filter( ressurs -> ressurs.personident().length() == 11 || isLocal) // Unntatt ved lokalkjøring eller test run
                    .map(ressurs -> new LuceneRessurs(ressurs.navident(),
                            ressurs.visningsnavn().getFormattedVisningsnavn(),
                            "tlf-nr",
                            "epost",
                            ressurs.sluttdato().map(date -> date.isAfter(LocalDate.now())).orElse(true),
                            ressurs.sektor().toString()
                            )
                    );

            wrappedLucene.upsertEntities(luceneRessursStream, LuceneRessurs.class);
        }

        {
            final var luceneOrgEnhetStream = grabAndResetOrgenhetInvalidationList().stream()
                    .map(orgenhetService::getById)
                    .map(Optional::get)
                    .map(orgenhet -> new LuceneOrgEnhet(
                            orgenhet.getId(),
                            orgenhet.getNavn(),
                            orgenhet.getNomNivaa().map(NomNivaa::name).orElse(null),
                            orgenhet.getOrgNiv(),
                            orgenhet.getGyldigTom()
                    ));



            wrappedLucene.upsertEntities(luceneOrgEnhetStream, LuceneOrgEnhet.class);
        }

        log.info("Lucene indexing completed.");
        this.initialLoadComplete.set(true);

    }

    //@Scheduled(initialDelayString = "PT20S", fixedRateString = "PT3H")
    public void refreshOrgenhet() {
        log.info("Lucene indexing started for orgenhet");
        wrappedLucene.upsertEntities(
            orgenhetService.getAllEnheter().stream()
                .map(orgenhet -> new LuceneOrgEnhet(
                        orgenhet.getFid(),
                        orgenhet.getNavn(),
                        orgenhet.getNomNivaa().toString(),
                        orgenhet.getOrgNiv(),
                        orgenhet.getGyldigTom()
                    )
                ), LuceneOrgEnhet.class);
        log.info("Lucene indexing for orgenhet completed.");

    }

    public synchronized void invalidateRessurs(String navident) {
        this.invalidatedNavidentList.add(navident);
    }

    public synchronized void invalidateOrgenhet(String orgenhetId) {
        invalidatedOrgEnhetList.add(orgenhetId);
    }

    private synchronized List<String> grabAndResetNavidentInvalidationList() {
        var out = this.invalidatedNavidentList;
        this.invalidatedNavidentList = new ArrayList<>();
        return out;
    }

    private synchronized List<String> grabAndResetOrgenhetInvalidationList() {
        var out = this.invalidatedOrgEnhetList;
        this.invalidatedOrgEnhetList = new ArrayList<>();
        return out;
    }

    public List<SearchResult> search(String term) {
        return wrappedLucene.search(term);
    }

    public List<SearchResult> searchRessurs(String term) {
        return searchRessurs(term, LuceneRessursFilter.alle());
    }

    public List<SearchResult> searchRessurs(String term, LuceneRessursFilter filter) {
        return search(term, filter, WrappedLucene.DEFAULT_RESULT_SIZE, false);
    }

    public List<SearchResult> searchRessurs(String term, LuceneRessursFilter filter, Integer resultLimit) {
        return search(term, filter, resultLimit, false);
    }

    public List<SearchResult> searchOrgenhet(String term) {
        return searchOrgenhet(term, LuceneOrgenhetFilter.alle());
    }


    public List<SearchResult> searchOrgenhet(String term, LuceneOrgenhetFilter filter) {
        return search(term, filter, WrappedLucene.DEFAULT_RESULT_SIZE, false);
    }
    public List<SearchResult> searchOrgenhet(String term, LuceneOrgenhetFilter filter, Integer resultLimit) {
        return search(term, filter, resultLimit, false);
    }

    private <T> List<SearchResult> search(String term, QueryFilter<T> filter, Integer resultLimit, boolean includeDefault) {
        var filterContext = new QueryFilterContext(filter);
        filterContext.setDefaultInclude(includeDefault);

        return wrappedLucene.search(term, filterContext, resultLimit);
    }

    public boolean filterOrgenhetResult(SearchResult searchResult) {
        return searchResult.entityType().equals(this.orgenhetEntityType);
    }

    public boolean filterRessursResult(SearchResult searchResult) {
        return searchResult.entityType().equals(this.ressursEntityType);
    }

    public synchronized boolean initLoadComplete() {
        return initialLoadComplete.get();
    }


    public void clear() {
        this.wrappedLucene.dropPersistedIndex();
    }
}

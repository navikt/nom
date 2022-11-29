package no.nav.nom.apps.api.lucenesearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LuceneService {

    private final WrappedLucene wrappedLucene;

    private final IRessursService ressursService;

    private List<String> invalidatedNavidentList = new ArrayList<>();

    private final OrgenhetService orgenhetService;

    private final String entTypeOrgenhet;
    private final String entTypeRessurs;

    public LuceneService(IRessursService ressursService, OrgenhetService orgenhetService) {
        this.orgenhetService = orgenhetService;
        var indexOrgEnhet = new OrgEnhetIndexer();
        var indexRessurs = new RessursIndexer();
        this.wrappedLucene = new WrappedLucene(List.of(indexOrgEnhet, indexRessurs));

        this.entTypeOrgenhet = indexOrgEnhet.typeStr();
        this.entTypeRessurs = indexRessurs.typeStr();

        this.ressursService = ressursService;
    }

    @Scheduled(initialDelayString = "PT10S", fixedRateString = "PT30S")
    public void addInvalidatedEntitiesToIndex() {
        var ressurserCountToIndex = this.invalidatedNavidentList.size();
        var orgenheterCountToIndex = 0;
        var sumCount = ressurserCountToIndex + orgenheterCountToIndex;
        if (sumCount == 0) {
            return;
        }
        log.info("Lucene indexing started. Number of entities:" + sumCount);

        // ressurser
        {
            var invalidatedNavIdentList = grabAndResetNavidentInvalidationList();

            var resMap = ressursService.findByNavidentBulk(invalidatedNavIdentList);
            var luceneRessursStream = resMap.values().stream()
                    .map(it -> {
                             var visningsNavn = it.visningsnavn().fornavn()
                                     + it.visningsnavn().mellomnavn().map(mellomnavn -> " " + mellomnavn).orElse("")
                                     + (" " + it.visningsnavn().etternavn());
                             var activeRessurs = it.sluttdato().isEmpty() || it.sluttdato().get().isAfter(LocalDate.now());
                             return new LuceneRessurs(it.navident(), visningsNavn, "tlf-nr", "epost", activeRessurs);
                         }
                    );

            wrappedLucene.upsertEntities(luceneRessursStream, LuceneRessurs.class);

            invalidatedNavidentList.clear(); // or just leave for GC?
        }

        // orgenheter
        {
            //            var invalidatedOrgEnhetList = grabAndResetOrgenhetInvalidationList();
            //            var oeMap = orgenhetService.findByIdBulk(invalidatedOrgEnhetList);
            //            var luceneOeStream = oeMap.values().stream().map(oe -> new LuceneOrgEnhet(oe.getId(), oe.getNavn().get(), oe.getEnhetsType()));
            //
            //            searchRepo.upsertEntities(luceneOeStream, LuceneOrgEnhet.class);
            //
            //            invalidatedOrgEnhetList.clear();
        }


        log.info("Lucene indexing completed.");

    }

    @Scheduled(initialDelayString = "PT20S", fixedRateString = "PT3H")
    public void refreshOrgenhet() {
        log.info("Lucene indexing started for orgenhet");
        wrappedLucene.upsertEntities(
                orgenhetService.getAllEnheter().stream()
                        .map(it -> new LuceneOrgEnhet(
                                     it.getFid().toString(),
                                     it.getNavn(),
                                     it.getOrgEnhetsKategori().toString()
                             )
                        ), LuceneOrgEnhet.class);
        log.info("Lucene indexing for orgenhet completed.");

    }

    public synchronized void invalidateRessurs(String navident) {
        this.invalidatedNavidentList.add(navident);
    }

    public synchronized void invalidateOrgenhet(String orgenhetId) {
        throw new NotImplementedException();
    }

    private synchronized List<String> grabAndResetNavidentInvalidationList() {
        var out = this.invalidatedNavidentList;
        this.invalidatedNavidentList = new ArrayList<>();
        return out;
    }

    private synchronized List<String> grabAndResetOrgenhetInvalidationList() {
        throw new NotImplementedException();
    }


    public List<SearchResult> search(String term) {
        return this.wrappedLucene.search(term);
    }

    public boolean filterOrgenhetResult(SearchResult searchResult) {
        return searchResult.entType().equals(this.entTypeOrgenhet);
    }

    public boolean filterRessursResult(SearchResult searchResult) {
        return searchResult.entType().equals(this.entTypeRessurs);
    }

}

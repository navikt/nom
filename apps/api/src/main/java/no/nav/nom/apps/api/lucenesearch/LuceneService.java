package no.nav.nom.apps.api.lucenesearch;

import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.search.SearchRepo;
import no.nav.nom.libs.lucenewrapper.search.SearchResult;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneService {

    private final SearchRepo searchRepo;

    private final UnleashRessursServiceSelector ressursService;

    private List<String> invalidatedNavidentList = new ArrayList<>();

    private final OrgenhetService orgenhetService;


    public LuceneService(UnleashRessursServiceSelector ressursService, OrgenhetService orgenhetService) {
        this.orgenhetService = orgenhetService;
        var indexOrgEnhet = new OrgEnhetIndexer();
        var indexRessurs = new RessursIndexer();
        this.searchRepo = new SearchRepo(List.of(indexOrgEnhet, indexRessurs));

        this.ressursService = ressursService;

    }

    @Scheduled(initialDelay = 5000L, fixedRate = 30_000L)
    public void addInvalidatedEntitiesToIndex(){

        // ressurser
        {
            var invalidatedNavIdentList = grabAndResetNavidentInvalidationList();

            var resMap = ressursService.findByNavidentBulk(invalidatedNavIdentList);
            var luceneRessursStream = resMap.values().stream().map(it -> {
                        var visningsNavn = it.getVisningsfornavn() + " "
                                + it.getVisningsmellomnavn().map(mellomnavn -> mellomnavn + " ").orElse("")
                                + it.getVisningsetternavn();
                        return new LuceneRessurs(it.getNavIdent(), visningsNavn, "tlf-nr", "epost");
                    }
            );

            searchRepo.upsertEntities(luceneRessursStream, LuceneRessurs.class);

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


    }

    public synchronized void invalidateRessurs(String navident){
        this.invalidatedNavidentList.add(navident);
    }

    public synchronized void invalidateOrgenhet(String orgenhetId){
        throw new NotImplementedException();
    }

    private synchronized List<String> grabAndResetNavidentInvalidationList(){
        var out = this.invalidatedNavidentList;
        this.invalidatedNavidentList = new ArrayList<>();
        return out;
    }

    private synchronized List<String> grabAndResetOrgenhetInvalidationList(){
        throw new NotImplementedException();
    }


    public List<SearchResult> search(String term){
        return this.searchRepo.search(term);
    }


}

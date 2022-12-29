package no.nav.nom.libs.lucenewrapper;

import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.search.GenericEntityIndexer;
import no.nav.nom.libs.lucenewrapper.search.QueryFilter;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

public class MainTest {

    private final LuceneRessurs r1 = new LuceneRessurs("A000001","Are Aslesen","","");
    private final LuceneRessurs r2 = new LuceneRessurs("A000002","Are Tollmann");
    private final LuceneOrgEnhet oe1 = new LuceneOrgEnhet("1","NAV Town 1","Vanlig");
    private final LuceneOrgEnhet oe2 = new LuceneOrgEnhet("2","NAV City 2","Vanlig");

    private final OrgEnhetIndexer indexOrgEnhet = new OrgEnhetIndexer();

    private final RessursIndexer indexRessurs = new RessursIndexer();

    @Test
    void twoHitsTwoExcluded(){
        var res = Helpers.initScenario(List.of(r1,r2,oe1,oe2)).search("Are");
        Assertions.assertEquals(2, res.size());
    }

    @Test
    void oneHitThreeExcluded(){
        var res = Helpers.initScenario(List.of(r1,r2,oe1,oe2)).search("Aslesen");
        Assertions.assertEquals(1, res.size());
    }

    @Test
    void edgeNgramHit(){
        var res = Helpers.initScenario(List.of(r1,r2,oe1,oe2)).search("Asle");
        Assertions.assertEquals(1, res.size());
    }

    @Test
    void twoHitsTwoExcludedOrgenhet(){
        var res = Helpers.initScenario(List.of(r1,r2,oe1,oe2)).search("NAV");
        Assertions.assertEquals(2,res.size());
    }

    @Test
    void twoHitsCorrectType(){
        var wantedIds = Stream.of(r1,r2).map(LuceneRessurs::navident).toList();
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2));
        var resList = repo.search("Are");
        Helpers.assertRetrievedIds(wantedIds, indexRessurs.typeString() , resList);
    }

    @Test
    void phraseHitRessurs(){
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2));
        var resList = repo.search("Are GY Asles");
        Helpers.assertBestMatch(Helpers.fromRessurs(r1, indexRessurs.typeString()) , resList);
    }

    @Test
    void phraseHitOrgenhet(){
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2));
        var resList = repo.search("NAV");
        var orgEnhetTypeStr = indexOrgEnhet.typeString();
        Helpers.assertRetrievedIds(Stream.of(oe1,oe2).map(it -> Helpers.fromOrgenhet(it,orgEnhetTypeStr)).toList() , resList);
    }

    @Test
    void fuzzySearch(){
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2));
        var resList = repo.search("Aere Aslesn");
        Assertions.assertTrue(resList.size() > 0, "results not empty");
    }

    @Test
    void genericDataSearch(){
        var entIndexer = new GenericEntityIndexer<>(
                GenericData.class,
                entity -> entity.id,
                entity -> entity.name,
                "t:GenericData",
                GenericDataFilter.class
        );

        var repo = new WrappedLucene(List.of(entIndexer));

        var one = new GenericData("1", "one", 5);
        var two = new GenericData("2", "two", 67);
        var three = new GenericData("3", "actually three", 12);

        repo.upsertEntity(one);
        repo.upsertEntities(Stream.of(two,three),GenericData.class);

        var resList = repo.search("tw");
        Assertions.assertEquals(1, resList.size());
        Assertions.assertEquals(two.id, resList.get(0).id());

        var resList2 = repo.search("one");
        Assertions.assertEquals(1, resList2.size());
        Assertions.assertEquals(one.id, resList2.get(0).id());
    }

    record GenericData(String id, String name, int number){

    }

    record GenericDataFilter() implements QueryFilter<GenericData> {

        @Override
        public Class<GenericData> getEntityClass() {
            return GenericData.class;
        }

        @Override
        public Mode mode() {
            return Mode.INCLUDE;
        }
    }

}

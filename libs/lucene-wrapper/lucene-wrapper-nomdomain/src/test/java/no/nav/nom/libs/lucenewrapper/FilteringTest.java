package no.nav.nom.libs.lucenewrapper;

import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgenhetFilter;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessursFilter;
import no.nav.nom.libs.lucenewrapper.search.QueryFilterContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

public class FilteringTest {

    LuceneRessurs r1 = new LuceneRessurs("1", "peter pack", "1", "1", true,"NAV_STATLIG");
    LuceneRessurs r2 = new LuceneRessurs("2", "peter pack", "2", "2", false,"NAV_STATLIG");
    LuceneRessurs r3 = new LuceneRessurs("3", "peter pack", "3", "3", true,"EKSTERN");

    LuceneOrgEnhet o1 = new LuceneOrgEnhet("oe1","nav x","t","ORGENHET", LocalDate.now().plusDays(1));
    LuceneOrgEnhet o2 = new LuceneOrgEnhet("oe2","nav x","t","ORGNIV4", LocalDate.now().minusDays(1));

    @Test
    void filterOnActiveRessurs() {
        var lucene = Helpers.initScenario(List.of(r1, r2));
        //        var res = x.search("peter",List.of(new LuceneRessursFilter(true, LuceneRessursSektor.STATLIG)));
        var res = lucene.search("peter",
                new QueryFilterContext(List.of(new LuceneRessursFilter(LuceneRessursFilter.StatusSelection.AKTIV, LuceneRessursFilter.SektorSelection.ALLE)))
        );
        //        System.out.println(res.stream().map(Record::toString).collect(Collectors.joining(",\n")));

        Assertions.assertEquals(1, res.size());
    }


    @Test
    void filterOnRessursSektor(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3));
        var res = lucene.search("peter",
            new QueryFilterContext(List.of(new LuceneRessursFilter(LuceneRessursFilter.StatusSelection.AKTIV, LuceneRessursFilter.SektorSelection.EKSTERN)))
        );

        Assertions.assertEquals(1, res.size());

    }


    @Test
    void filterOnRessursSektor2(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3));

        var res2 = lucene.search("peter",
            new QueryFilterContext(List.of(new LuceneRessursFilter(LuceneRessursFilter.StatusSelection.ALLE, LuceneRessursFilter.SektorSelection.STATLIG)))
        );

        Assertions.assertEquals(2, res2.size());

    }

    @Test
    void filterOnRessursSektor3(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3));

        var res2 = lucene.search("peter",
            new QueryFilterContext(List.of(new LuceneRessursFilter(LuceneRessursFilter.StatusSelection.ALLE, LuceneRessursFilter.SektorSelection.IKKE_EKSTERN)))
        );

        Assertions.assertEquals(2, res2.size());
    }

    @Test
    void filterOnOrgenhetNiva(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3,o1,o2));

        var res2 = lucene.search("nav x",
            new QueryFilterContext(List.of(new LuceneOrgenhetFilter(LuceneOrgenhetFilter.GyldigTomSelection.ALLE, LuceneOrgenhetFilter.OrgnivSelection.ORGNIV_ORGENHET)))
        );

        Assertions.assertEquals(o1.id(),res2.get(0).id());
    }


    @Test
    void filterOnOrgenhetNiva2(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3,o1,o2));

        var res2 = lucene.search("nav x",
            new QueryFilterContext(List.of(new LuceneOrgenhetFilter(LuceneOrgenhetFilter.GyldigTomSelection.ALLE, LuceneOrgenhetFilter.OrgnivSelection.IKKE_ORGNIV_ORGENHET)))
        );

        Assertions.assertEquals(o2.id(),res2.get(0).id());
    }

    void filterOnOrgenhetNiva3(){
        var lucene = Helpers.initScenario(List.of(r1, r2,r3,o1,o2));

        var res2 = lucene.search("nav x",
            new QueryFilterContext(List.of(new LuceneOrgenhetFilter(LuceneOrgenhetFilter.GyldigTomSelection.ALLE, LuceneOrgenhetFilter.OrgnivSelection.ALLE)))
        );

        Assertions.assertEquals(2,res2.size());
    }

}

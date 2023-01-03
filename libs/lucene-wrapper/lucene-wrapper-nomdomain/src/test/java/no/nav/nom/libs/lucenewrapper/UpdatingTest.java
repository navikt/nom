package no.nav.nom.libs.lucenewrapper;

import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UpdatingTest {
    LuceneRessurs r1 = new LuceneRessurs("A1","Are Aslesen");
    LuceneRessurs r1_2 = new LuceneRessurs("A1","Rick Rudy");
    LuceneRessurs r2 = new LuceneRessurs("A2","Ulf Olsen");

    LuceneOrgEnhet oe1 = new LuceneOrgEnhet("oe1","NAV City 1","Vanlig");
    LuceneOrgEnhet oe1_2 = new LuceneOrgEnhet("oe1","NAV Rink 4","Vanlig");
    LuceneOrgEnhet oe2 = new LuceneOrgEnhet("oe2","NAV Town 1","Vanlig");
    LuceneOrgEnhet oe3 = new LuceneOrgEnhet("oe3","NAV Iceberg 1","Vanlig");

    private final OrgEnhetIndexer indexOrgEnhet = new OrgEnhetIndexer();

    private final RessursIndexer indexRessurs = new RessursIndexer();

    @Test
    void updateWorksForRessurs(){
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2,oe3));
        var resList = repo.search(r1.navn());
        Helpers.assertBestMatch(Helpers.fromRessurs(r1, indexRessurs.typeString()), resList);
        repo.upsertEntity(r1_2);
        var resList2 = repo.search(r1.navn());
        Assertions.assertFalse(resList2.stream().anyMatch(it -> it.id().equals(r1.navident())));

        Assertions.assertEquals(repo.search(r1_2.navn()).stream()
                .filter(it -> it.id().equals(r1.navident()))
                .findFirst().get().name(), r1_2.navn());

    }

    @Test
    void updatedWorksForEnhet(){
        var repo = Helpers.initScenario(List.of(r1,r2,oe1,oe2,oe3));
        var resList = repo.search(oe1.navn());
        Helpers.assertBestMatch(Helpers.fromOrgenhet(oe1, indexOrgEnhet.typeString()), resList);
        repo.upsertEntity(oe1_2);
        var resList2 = repo.search(oe1.navn());
        Assertions.assertTrue(resList2.size() == 0 || !resList2.get(0).id().equals(oe1.id()));

        var resList3 = repo.search(oe1_2.navn());
        Assertions.assertTrue(resList3.size() != 0 && resList3.get(0).id().equals(oe1.id()));

    }
}

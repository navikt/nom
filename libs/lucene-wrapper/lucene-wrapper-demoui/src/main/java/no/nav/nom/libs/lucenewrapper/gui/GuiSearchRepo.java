package no.nav.nom.libs.lucenewrapper.gui;

import no.nav.nom.libs.lucenewrapper.domain.OrgEnhetIndexer;
import no.nav.nom.libs.lucenewrapper.domain.RessursIndexer;
import no.nav.nom.libs.lucenewrapper.domain.LuceneOrgEnhet;
import no.nav.nom.libs.lucenewrapper.domain.LuceneRessurs;
import no.nav.nom.libs.lucenewrapper.search.WrappedLucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class GuiSearchRepo {

    private static final RessursIndexer indexRessurs = new RessursIndexer();
    private static final OrgEnhetIndexer indexOrgEnhet = new OrgEnhetIndexer();

    public static WrappedLucene guiSearchRepo() {
        var out = new WrappedLucene(List.of(indexRessurs, indexOrgEnhet));

        boolean ressurserFileLoadOk = false;
        boolean enheterFileLoadOk = false;
        try {
            ressurserFileLoadOk = loadRessurserFromFile(out);
            enheterFileLoadOk = loadEnheterFromFile(out);
        } catch (IOException ignored) {

        } finally {
            if (!ressurserFileLoadOk) {
                loadRessurserFromCode(out);
            }
            if (!enheterFileLoadOk) {
                loadEnheterFromCode(out);
            }
        }

        return out;

    }

    private static boolean loadRessurserFromFile(WrappedLucene repo) throws IOException {
        var url = GuiSearchRepo.class.getClassLoader().getResource("ressurserForLucene.txt");
        if (url == null) {
            return false;
        }
        var f = new File(url.getFile());
        BufferedReader br = new BufferedReader(new FileReader(f));

        var ressursStream = br.lines()
                .filter(it -> !it.isEmpty())
                .map(it -> {
                    var row = it.split("\t");
                    var artificalActiveSplit = Integer.parseInt(row[0].substring(1)) > 150000;
                    return new LuceneRessurs(row[0], row[1], artificalActiveSplit);
                });
        repo.upsertEntities(ressursStream, indexRessurs);

        return true;
    }

    private static boolean loadEnheterFromFile(WrappedLucene repo) throws FileNotFoundException {
        var url = GuiSearchRepo.class.getClassLoader().getResource("orgenheterForLucene.txt");
        if (url == null) {
            return false;
        }
        var f = new File(url.getFile());
        BufferedReader br = new BufferedReader(new FileReader(f));

        var orgEnhetStream = br.lines()
                .filter(it -> !it.isEmpty())
                .map(it -> {
                    var row = it.split("\t");
                    return new LuceneOrgEnhet(row[0], row[1], "status: " + row[2]);
                });
        repo.upsertEntities(orgEnhetStream, indexOrgEnhet);

        return true;
    }

    private static void loadRessurserFromCode(WrappedLucene repo) {
        repo.upsertEntity(new LuceneRessurs("A0", "Leif Hansen"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A1", "Tore Pettersen"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A2", "Geir Nesby"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A3", "Hans Treholt"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A4", "Lise Treskehus"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A5", "Vanja Ulsnes"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A6", "Vidar Ulsnes"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A7", "Are Pettersen"), indexRessurs);
        repo.upsertEntity(new LuceneRessurs("A8", "Åshild Østskæret"), indexRessurs);
    }

    private static void loadEnheterFromCode(WrappedLucene repo) {
        repo.upsertEntity(new LuceneOrgEnhet("oe1", "NAV Oslo", "Vanlig"), indexOrgEnhet);
        repo.upsertEntity(new LuceneOrgEnhet("oe2", "NAV Bergen", "Vanlig"), indexOrgEnhet);
        repo.upsertEntity(new LuceneOrgEnhet("oe3", "NAV Trondheim", "Vanlig"), indexOrgEnhet);
        repo.upsertEntity(new LuceneOrgEnhet("oe4", "NAV Tromsø", "Vanlig"), indexOrgEnhet);
        repo.upsertEntity(new LuceneOrgEnhet("oe5", "NAV Oslo Gamle", "Vanlig"), indexOrgEnhet);
        repo.upsertEntity(new LuceneOrgEnhet("oe6", "NAV Oslo Nye", "Vanlig"), indexOrgEnhet);
    }
}

package no.nav.nom.apps.api.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class CompareOldAndNewOrgtilknytningStates {

    private final OrgtilknytningDvhService orgtilknytningDvhService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final RessursStateRepository ressursStateRepository;
    private final HashMap<String, Pair> diffAntallOT = new HashMap<>();
    private final HashMap<String, Pair> diffAntallOT_ExcludeEksterne = new HashMap<>();
    private final HashMap<String, List<Orgtilknytning>> gamleOTerIkkeFunnetBlandtNye = new HashMap<>();
    private final HashMap<String, List<Orgtilknytning>> gamleOTerIkkeFunnetBlandtNye_ExcludeEksterne = new HashMap<>();
    private final HashMap<String, List<Orgtilknytning>> gamleOTerFunnetBlandtNye_DatoDiff = new HashMap<>();
    private final HashMap<String, List<Orgtilknytning>> gamleOTerFunnetBlandtNye_DatoDiff_ExcludeEksterne = new HashMap<>();
    private final HashMap<String, List<OrgtilknytningState>> nyeOTerIkkeFunnetBlandtGamle = new HashMap<>();
    private final HashMap<String, List<OrgtilknytningState>> nyeOTerIkkeFunnetBlandtGamle_ExcludeEksterne = new HashMap<>();
    private final HashMap<String, List<OrgtilknytningState>> nyeOTerFunnetBlandtGamle_DatoDiff = new HashMap<>();
    private final HashMap<String, List<OrgtilknytningState>> nyeOTerFunnetBlandtGamle_DatoDiff_ExcludeEksterne = new HashMap<>();

    private boolean isDone = false;
    private boolean isStarted = false;

    public CompareOldAndNewOrgtilknytningStates(OrgtilknytningDvhService orgtilknytningDvhService, OrgtilknytningStateService orgtilknytningStateService, RessursStateRepository ressursStateRepository) {
        this.orgtilknytningDvhService = orgtilknytningDvhService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.ressursStateRepository = ressursStateRepository;
    }

    @Async
    public void start() {
        if (isStarted) return;
        log.info("Started sammenligning på gml vs ny orgtilknytning");
        isDone = false;
        isStarted = true;
        ressursStateRepository.getAll().forEach(this::compareRessurs);
        isDone = true;
        isStarted = false;
        log.info("Ferdig med sammenligning på gml vs ny orgtilknytning");
    }

    public String OppsummertRapport() {
        if (!isDone) return "Ikke ferdig generert resultat. Generering kjører: " + isStarted;

        StringBuffer strBuf = new StringBuffer();
        lagOppsummertRapport(strBuf);
        return strBuf.toString();
    }

    public String detaljertRapport(boolean inkluderEksterne) {
        if (!isDone) return "Ikke ferdig generert resultat. Generering kjører: " + isStarted;

        StringBuffer strBuf = new StringBuffer();
        lagOppsummertRapport(strBuf);
        ressursStateRepository.getAll().forEach( ressursState -> detaljerPerNavident(ressursState, strBuf, inkluderEksterne));
        return strBuf.toString();
    }

    private void lagOppsummertRapport(StringBuffer resultat) {
        resultat.append("Antall differanse orgtilknytninger gamle vs nye: %5s\n".formatted(diffAntallOT.values().stream().reduce(0, (acc, pair) -> acc + Math.abs(pair.diff()), Integer::sum)));
        resultat.append("                                  uten eksterne: %5s\n".formatted(diffAntallOT_ExcludeEksterne.values().stream().reduce(0, (acc, pair) -> acc + Math.abs(pair.diff()), Integer::sum)));
        resultat.append("\n");
        resultat.append("Antall gamle orgtilknytninger ikke funnet i nye: %5s\n".formatted(gamleOTerIkkeFunnetBlandtNye.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                                  uten eksterne: %5s\n".formatted(gamleOTerIkkeFunnetBlandtNye_ExcludeEksterne.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                          se bort fra dato diff: %5s\n".formatted(gamleOTerFunnetBlandtNye_DatoDiff.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                                  uten eksterne: %5s\n".formatted(gamleOTerFunnetBlandtNye_DatoDiff_ExcludeEksterne.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("\n");
        resultat.append("Antall nye orgtilknytninger ikke funnet i gamle: %5s\n".formatted(nyeOTerIkkeFunnetBlandtGamle.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                                  uten eksterne: %5s\n".formatted(nyeOTerIkkeFunnetBlandtGamle_ExcludeEksterne.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                          se bort fra dato diff: %5s\n".formatted(nyeOTerFunnetBlandtGamle_DatoDiff.values().stream().mapToLong(Collection::size).sum()));
        resultat.append("                                  uten eksterne: %5s\n".formatted(nyeOTerFunnetBlandtGamle_DatoDiff_ExcludeEksterne.values().stream().mapToLong(Collection::size).sum()));
    }

    private void detaljerPerNavident(RessursState ressursState, StringBuffer resultat, boolean inkluderEksterne) {
        if (!inkluderEksterne && ressursState.sektor() == Sektor.EKSTERN) return;

        String navident = ressursState.navident();
        resultat.append("Detaljer for %s:\n".formatted(navident));

        resultat.append("    Gamle orgtilknytninger:\n");
        orgtilknytningDvhService.getKoblingerByNavIdentOld(navident).stream().forEach( gml -> resultat.append("        %s".formatted(gml)));

        resultat.append("    Nye orgtilknytninger:\n");
        orgtilknytningStateService.getOrgtilknytningerByNavIdent(navident).stream().forEach( nye -> resultat.append("        %s".formatted(nye)));

        List<Orgtilknytning> kunGamle = inkluderEksterne ? gamleOTerIkkeFunnetBlandtNye.get(navident) : gamleOTerIkkeFunnetBlandtNye_ExcludeEksterne.get(navident);
        resultat.append("Antall gamle orgtilknytninger ikke funnet i nye: %5s\n".formatted(kunGamle.size()));
        kunGamle.forEach( gml -> resultat.append("        %s\n".formatted(gml)) );

        List<Orgtilknytning> kunGamleUtenDatoDiff = inkluderEksterne ? gamleOTerFunnetBlandtNye_DatoDiff.get(navident) : gamleOTerFunnetBlandtNye_DatoDiff_ExcludeEksterne.get(navident);
        resultat.append("Antall gamle orgtilknytninger ikke funnet i nye sett bort fra dato diff: %5s\n".formatted(kunGamleUtenDatoDiff.size()));
        kunGamleUtenDatoDiff.forEach( gml -> resultat.append("        %s\n".formatted(gml)) );

        List<OrgtilknytningState> kunNye = inkluderEksterne ? nyeOTerIkkeFunnetBlandtGamle.get(navident) : nyeOTerIkkeFunnetBlandtGamle_ExcludeEksterne.get(navident);
        resultat.append("Antall nye orgtilknytninger ikke funnet i nye: %5s\n".formatted(kunNye.size()));
        kunNye.forEach( ny -> resultat.append("        %s\n".formatted(ny)) );

        List<OrgtilknytningState> kunNyeUtenDatoDiff = inkluderEksterne ? nyeOTerFunnetBlandtGamle_DatoDiff.get(navident) : nyeOTerFunnetBlandtGamle_DatoDiff_ExcludeEksterne.get(navident);
        resultat.append("Antall nye orgtilknytninger ikke funnet i nye sett bort fra dato diff: %5s\n".formatted(kunNyeUtenDatoDiff.size()));
        kunNyeUtenDatoDiff.forEach( ny -> resultat.append("        %s\n".formatted(ny)) );
    }

    private void compareRessurs(RessursState ressursState) {
        var gamleOTer = orgtilknytningDvhService.getKoblingerByNavIdentOld(ressursState.navident());
        var nyeOTer = orgtilknytningStateService.getOrgtilknytningerByNavIdent(ressursState.navident());

        sizeDiff(ressursState, gamleOTer, nyeOTer);

        finnGamleOTerSomManglerINyeOTer(ressursState, gamleOTer, nyeOTer);
        finnNyeOTerSomManglerIGamleOTer(ressursState, gamleOTer, nyeOTer);
    }

    private void finnGamleOTerSomManglerINyeOTer(RessursState ressursState, List<Orgtilknytning> gamleOTer, Collection<OrgtilknytningState> nyeOTer) {
        String navident = ressursState.navident();
        gamleOTerIkkeFunnetBlandtNye.put(navident, new ArrayList<>());
        gamleOTerIkkeFunnetBlandtNye_ExcludeEksterne.put(navident, new ArrayList<>());
        gamleOTerFunnetBlandtNye_DatoDiff.put(navident, new ArrayList<>());
        gamleOTerFunnetBlandtNye_DatoDiff_ExcludeEksterne.put(navident, new ArrayList<>());
        gamleOTer.stream()
            .filter( gammel -> !gammelOTFinnesINyeOTer(gammel, nyeOTer))
            .forEach( gammel -> {
                gamleOTerIkkeFunnetBlandtNye.get(navident).add(gammel);
                if (ressursState.sektor() != Sektor.EKSTERN) {
                    gamleOTerIkkeFunnetBlandtNye_ExcludeEksterne.get(navident).add(gammel);
                }
                if (gammelOTFinnesINyeOTer_ikkeSjekkDato(gammel, nyeOTer)) {
                    gamleOTerFunnetBlandtNye_DatoDiff.get(navident).add(gammel);
                    if (ressursState.sektor() != Sektor.EKSTERN) {
                        gamleOTerFunnetBlandtNye_DatoDiff_ExcludeEksterne.get(navident).add(gammel);
                    }
                }
            });
    }

    private void finnNyeOTerSomManglerIGamleOTer(RessursState ressursState, List<Orgtilknytning> gamleOTer, Collection<OrgtilknytningState> nyeOTer) {
        String navident = ressursState.navident();
        nyeOTerIkkeFunnetBlandtGamle.put(navident, new ArrayList<>());
        nyeOTerIkkeFunnetBlandtGamle_ExcludeEksterne.put(navident, new ArrayList<>());
        nyeOTerFunnetBlandtGamle_DatoDiff.put(navident, new ArrayList<>());
        nyeOTerFunnetBlandtGamle_DatoDiff_ExcludeEksterne.put(navident, new ArrayList<>());

        nyeOTer.stream()
            .filter( ny -> !nyOTFinnesIGamleOTer(ny, gamleOTer))
            .forEach( ny -> {
                nyeOTerIkkeFunnetBlandtGamle.get(navident).add(ny);
                if (ressursState.sektor() != Sektor.EKSTERN) {
                    nyeOTerIkkeFunnetBlandtGamle_ExcludeEksterne.get(navident).add(ny);
                }
                if (nyOTFinnesIGamleOTer_ikkeSjekkDato(ny, gamleOTer)) {
                    nyeOTerFunnetBlandtGamle_DatoDiff.get(navident).add(ny);
                    if (ressursState.sektor() != Sektor.EKSTERN) {
                        nyeOTerFunnetBlandtGamle_DatoDiff_ExcludeEksterne.get(navident).add(ny);
                    }
                }
            });
    }

    private boolean nyOTFinnesIGamleOTer(OrgtilknytningState nyOT, Collection<Orgtilknytning> gamleOTer) {
        return gamleOTer.stream()
            .anyMatch( gammel ->
                gammel.getOrgEnhetId().equals(nyOT.orgEnhetId()) &&
                    gammel.getGyldigFom().equals(nyOT.gyldigFom()) &&
                    gammel.getGyldigTom().equals(nyOT.gyldigTom())
            );
    }

    private boolean nyOTFinnesIGamleOTer_ikkeSjekkDato(OrgtilknytningState nyOT, Collection<Orgtilknytning> gamleOTer) {
        return gamleOTer.stream()
            .anyMatch( gammel ->
                gammel.getOrgEnhetId().equals(nyOT.orgEnhetId())
            );
    }


    private boolean gammelOTFinnesINyeOTer(Orgtilknytning gammelOT, Collection<OrgtilknytningState> nyeOTer) {
        return nyeOTer.stream()
            .anyMatch( nyOt ->
                nyOt.orgEnhetId().equals(gammelOT.getOrgEnhetId()) &&
                    nyOt.gyldigFom().equals(gammelOT.getGyldigFom()) &&
                    nyOt.gyldigTom().equals(gammelOT.getGyldigTom())
            );
    }

    private boolean gammelOTFinnesINyeOTer_ikkeSjekkDato(Orgtilknytning gammelOT, Collection<OrgtilknytningState> nyeOTer) {
        return nyeOTer.stream()
            .anyMatch( nyOt ->
                nyOt.orgEnhetId().equals(gammelOT.getOrgEnhetId())
            );
    }

    private void sizeDiff(RessursState ressursState, List<Orgtilknytning> gamleOTer, Collection<OrgtilknytningState> nyeOTer) {
        if (gamleOTer.size()!= nyeOTer.size()) {
            var gammelVsNyeAntall = new Pair(gamleOTer.size(), nyeOTer.size());
            diffAntallOT.put(ressursState.navident(), gammelVsNyeAntall);
            if (ressursState.sektor() != Sektor.EKSTERN) {
                diffAntallOT_ExcludeEksterne.put(ressursState.navident(), gammelVsNyeAntall);
            }
        }
    }

    record Pair(int x, int y) {
        int diff() {return x -y;}
    };
}

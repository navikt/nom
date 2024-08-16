package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.NO_END_DATE;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayAfter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.dayBefore;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.etter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.foer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.lik;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerEtter;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.likEllerFoer;
import static no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil.overlapsInterval;
import static no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident.ErrorType.NAVIDENT_FINNES_IKKE;

@Component
@Slf4j
public class NyeRessursStillingStrukturerer {
    private final OrgenhetService orgenhetService;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public NyeRessursStillingStrukturerer(
        OrgenhetService orgenhetService, RessursStillingLoadStatus ressursStillingLoadStatus,
        RessursStateRepository ressursStateRepository, OrgtilknytningStateRepository orgtilknytningStateRepository
    ) {
        this.orgenhetService = orgenhetService;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public Map<String, StrukturerteStillingPerioderForNavident> strukturerStilingPerioderPerNavident(List<DvhRessursStilling> dvhRessursStillinger) {
        Map<String, List<DvhRessursStilling>> dvhRessursStillingPerNavident = grupperDvhRessursStillingerPerNavident(dvhRessursStillinger);

        Map<String, StrukturerteStillingPerioderForNavident> stillingPerioderPerNavident = new HashMap<>();
        dvhRessursStillingPerNavident.forEach((navident, navidentensDvhRessursStillinger) -> {
            stillingPerioderPerNavident.put(navident, strukturerStillingPerioderFraDvhRessursStillinger(navident, navidentensDvhRessursStillinger));
        });

        // stillingPerioderPerNavident har nå alle stillingPerioder for alle navidenter
        // Hvor sammenhengende perioder er slått sammen, korrekt NOM orgenhet er populert, og DOL er plassert, og evt feil og problemer identifisert
        return stillingPerioderPerNavident;
    }

    protected Map<String, List<DvhRessursStilling>> grupperDvhRessursStillingerPerNavident(List<DvhRessursStilling> dvhRessursStillinger) {
        return dvhRessursStillinger.stream()
            .filter(rs -> rs.getNavident().isPresent())
            .collect(groupingBy(rs -> rs.getNavident().get()));
    }


    protected StrukturerteStillingPerioderForNavident strukturerStillingPerioderFraDvhRessursStillinger(String navident, List<DvhRessursStilling> dvhRessursStillinger) {
        var sspForNavident = new StrukturerteStillingPerioderForNavident(navident, dvhRessursStillinger);

        if (!finnesNavidentINOM(sspForNavident)) {
            return sspForNavident;
        }

        grupperOgSlaaSammenDvhStillingperioder(dvhRessursStillinger, sspForNavident);
        matchStillingPerioderTilNomEnheter(sspForNavident);
        settKorrekteDOLPerioder(sspForNavident);

        // TODO: Sjekke for Ressursens SluttDato - ingen StillingPeriode utover dette
        // TODO: Sanity sjekk av StillingPerioder - ikke negative etc
        // TODO: Sjekk mismatch mellom aktiv stilling og enhet på ressurs i DvhRessursStillinger
        // TODO: Sjekk DvhRessursStillinger utover Ressursens periode

        return sspForNavident;
    }

    protected boolean finnesNavidentINOM(StrukturerteStillingPerioderForNavident sspForNavident) {
        String navident = sspForNavident.navident();
        if (ressursStateRepository.findByNavident(navident).isEmpty()) {
            log.error("Har ressursStillinger fra DVH for navident {} som ikke finnes i NOM", navident);
            sspForNavident.addError(
                NAVIDENT_FINNES_IKKE,
                "Har ressursStillinger fra DVH for navident %s som ikke finnes i NOM".formatted(navident)
            );
            return false;
        }
        return true;
    }

    protected void grupperOgSlaaSammenDvhStillingperioder(
        List<DvhRessursStilling> dvhRessursStillinger,
        StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        // grupper først på Stillingens - stilling enhet/navn +enhetNavn+ressurs (A+B)
        var grupperteDvhStillinger = dvhRessursStillinger.stream()
            .collect(groupingBy(rs ->
                new StillingPeriode.StillingAgressoOrgIdOgNavn(
                    rs.getStillingAgressoId(),
                    rs.getStillingOrgenhetNavn(),
                    rs.getRessursAgressoId(),
                    rs.getRessursOrgenhetNiv()
                )
            ));

        List<StillingPeriode> perioderGruppert = new ArrayList<>();

        var ressurs = ressursStateRepository.findByNavident(sspForNavident.navident()).get();
        LocalDate ressursStartdato = ressurs.startdato().orElse(null);
        LocalDate ressursSluttdato = ressurs.sluttdato().orElse(null);

        // Sorter og slå sammen sammenhengende perioder
        grupperteDvhStillinger.forEach((stillingAgressoOrgIdOgNavn, grupperteRessursStillinger) -> {
            var sorted = grupperteRessursStillinger.stream().sorted(Comparator.comparing(DvhRessursStilling::getStillingDatoFom)).toList();

            LocalDate periodStart = null;
            LocalDate previousEnd = null;

            for (DvhRessursStilling rs : sorted) {
                var start = rs.getStillingDatoFom();
                var end = rs.getStillingDatoTom();

                if (erLikEllerEtterRessursensSluttDato(start, ressursSluttdato)) {
                    lagErrorStartEtterRessursSluttdato(sspForNavident, rs, ressurs.navident(), ressursSluttdato);
                    continue; // Skipp denne DvhRessursStillings raden
                }

                if (erFoerRessursensStartDato(end, ressursStartdato)) {
                    lagErrorEndFoerRessursStartdato(sspForNavident, rs, ressurs.navident(), ressursStartdato);
                    continue; // Skipp denne DvhRessursStillings raden
                }

                if (periodStart == null) {
                    periodStart = start;
                    previousEnd = end;
                }

                if (erEtterRessursensSluttDato(end, ressursSluttdato)) {
                    lagErrorSlutterEtterRessursSluttdato(sspForNavident, rs, ressurs.navident(), ressursSluttdato);
                    if (end.isEqual(previousEnd)) {
                        previousEnd = ressursSluttdato;
                    }
                    end = ressursSluttdato;
                }

                if (erFoerRessursensStartDato(start, ressursStartdato)) {
                    lagErrorStartFoerRessursStartdato(sspForNavident, rs, ressurs.navident(), ressursStartdato);
                    if (start.isEqual(periodStart)) {
                        periodStart = ressursStartdato;
                    }
                    start = ressursStartdato;
                }

                if (end.isEqual(previousEnd)) {
                    // Kan være første DvhRessursStillings rad som er klippet på startdato ift ressursStartdato)
                    continue;
                } else if (start.isEqual(dayAfter(previousEnd))) {
                    //Stillings raden er fortsettelse på forrige rad - slå sammen
                    previousEnd = end;
                } else if (start.isAfter(previousEnd)) {
                    // Her er det brudd i stilling - Lag en StillingPeriode på forrige periode, og fortsett
                    perioderGruppert.add(
                        new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                    );
                    periodStart = start;
                    previousEnd = end;
                } else {
                    // Her er det overlapp i datoer på samme stillingAgressoId, stillingOrgenhetNavn og stillingRessursAgressoId
                    // Logg Error - og klipp bort overlappingen
                    lagErrorOverlappIPeriode(sspForNavident, rs, ressurs.navident(), stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end);

                    previousEnd = end;
                }
            }

            // Siste RessursStilling er slått sammen - Lag StillingsPeriode, hvis vi har en periode
            if (periodStart != null) {
                perioderGruppert.add(
                    new StillingPeriode(stillingAgressoOrgIdOgNavn, periodStart, previousEnd, null)
                );
            }
        });
        sspForNavident.addStillingPerioder(perioderGruppert.stream().sorted(Comparator.comparing(StillingPeriode::gyldigFom)).toList());
    }

    private boolean erLikEllerEtterRessursensSluttDato(LocalDate dato, LocalDate ressursSluttdato) {
        return ressursSluttdato != null && (dato.equals(ressursSluttdato) || dato.isAfter(ressursSluttdato));
    }

    private boolean erFoerRessursensStartDato(LocalDate dato, LocalDate ressursStartdato) {
        return ressursStartdato != null && dato.isBefore(ressursStartdato);
    }

    private boolean erEtterRessursensSluttDato(LocalDate dato, LocalDate ressursSluttdato) {
        return ressursSluttdato != null && dato.isAfter(ressursSluttdato);
    }

    private void lagErrorStartEtterRessursSluttdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursSluttdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s starter %s på eller etter ressursens sluttdato %s.
            Behandler ikke denne DVH RessursStillingen.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoFom(), ressursSluttdato),
            dvhRessursStilling
        );
    }

    private void lagErrorEndFoerRessursStartdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursStartdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s slutter %s før ressursens startdato %s.
            Behandler ikke denne DVH RessursStillingen.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(), ressursStartdato),
            dvhRessursStilling
        );
    }

    private void lagErrorSlutterEtterRessursSluttdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursSluttdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH ressursStilling for Navident %s har sluttdato %s etter ressursens sluttdato %s.
            Avslutter stillingen på ressursens sluttdato.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(),  ressursSluttdato),
            dvhRessursStilling
        );
    }

    private void lagErrorStartFoerRessursStartdato(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        LocalDate ressursStartdato
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLING_UTOVER_NAVIDENT_GYLDIGHET,
            """
            DVH RessursStilling for Navident %s slutter %s før ressursens startdato %s.
            Starter stillingen på ressursens startdato.
            """
                .formatted(navident, dvhRessursStilling.getStillingDatoTom(), ressursStartdato),
            dvhRessursStilling
        );
    }

    private void lagErrorOverlappIPeriode(
        StrukturerteStillingPerioderForNavident sspForNavident,
        DvhRessursStilling dvhRessursStilling,
        String navident,
        StillingPeriode.StillingAgressoOrgIdOgNavn stillingAgressoOrgIdOgNavn,
        LocalDate periodStart, LocalDate previousEnd,
        LocalDate start, LocalDate end
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DVH_RESSURSSTILLINGER_HAR_OVERLAPPENDE_PERIODER,
            "Navident %s har overlapp i stillingsdatoer for '%s' -> forrige periode [%s - %s] og denne stillingen [%s - %s]"
                .formatted(navident, stillingAgressoOrgIdOgNavn, periodStart, previousEnd, start, end),
            dvhRessursStilling
        );
    }


    protected void matchStillingPerioderTilNomEnheter(StrukturerteStillingPerioderForNavident sspForNavident) {
        String navident = sspForNavident.navident();
        List<StillingPeriode> matchetMedNOMenhet = new ArrayList<>();
        TreeMap<Integer, StillingPeriode> unmatchedPresisAgressoId = new TreeMap<>(); // Sorterer automagisk på key

        // StillingPeriodene er sortert på gyldigFom
        sspForNavident.stillingPerioder().forEach(stillingPeriode -> {
            // Benytter enten presis agressoId fra stillingen,
            // eller agressoId på ressursen (OrgenhetPaaRessurs), hvis denne matcher stillingens agressoId og er presis,
            // eller hvis det er en aktiv StillingsPeriode og det er en presis agressoId på ressursen (OrgenhetPaaRessurs)
            String presisAgressoId = stillingPeriode.getPresisAgressoId();
            if (presisAgressoId != null) {
                // Ref: 'Matche til enheter i NOM' 1. Presis enhet på Stilling (eller via tilsvarende 2.i/2.iii)
                var orgenheter = finnNomEnheterForEksaktIdIPerioden(presisAgressoId, stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom());

                if (orgenheter.size() == 1) {
                    var resultat = behandlePresisAgressoIdMedSingleMatchPaaNOMEnhet(stillingPeriode, orgenheter.getFirst(), sspForNavident);
                    matchetMedNOMenhet.add(resultat);
                } else if (orgenheter.size() > 1) {
                    List<StillingPeriode> resultat = behandlePresisAgressoIdMedMulitMatchPaaNOMEnhet(stillingPeriode, orgenheter, sspForNavident);
                    matchetMedNOMenhet.addAll(resultat);
                } else {
                    // Ref. 'Matche til enheter i NOM'  1.iii.a - Ingen match på presis agressoId

                    // Vi må ta vare på denne umatchede StillingPerioden - og behandle den etterpå. (For vi må sammenligne med alle matchede StillingsPerioder)
                    // Pass på plasseringen den har i sorteringen - vi ønsker å beholde rekkefølgen
                    var plassering = matchetMedNOMenhet.size();
                    unmatchedPresisAgressoId.put(plassering, stillingPeriode);
                }
            } else {
                OrgtilknytningState OTSomMatcherAgressoIdIPerioden = null;
                if (!stillingPeriode.manglerAgressoId()) {
                    // Vi har agressoId fra stilling men den er ikke presis
                    // Ref: 'Matche til enheter i NOM'  2.ii - Ingen match mellom stillingens agressoId og ressursAgressoId (OrgenhetPaaRessurs)
                    OTSomMatcherAgressoIdIPerioden = finnOTSomMatcherUpresisAgressoIdOgDekkerPerioden(
                        navident, stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(), stillingPeriode.stilling().agressoId().get()
                    );
                }
                if (OTSomMatcherAgressoIdIPerioden == null && stillingPeriode.isCurrentPeriod() && !stillingPeriode.manglerRessursAgressoId()) {
                    // Ref: 'Matche til enheter i NOM'  3.ii - Ingen match mellom stillingens agressoId og ressursAgressoId (OrgenhetPaaRessurs)
                    OTSomMatcherAgressoIdIPerioden =finnOTSomMatcherUpresisAgressoIdOgDekkerPerioden(
                        navident, stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(), stillingPeriode.stilling().ressursAgressoId().get()
                    );
                }
                if (OTSomMatcherAgressoIdIPerioden != null) {
                    matchetMedNOMenhet.add(
                        new StillingPeriode(
                            stillingPeriode.stilling(),
                            OTSomMatcherAgressoIdIPerioden.gyldigFom(),
                            OTSomMatcherAgressoIdIPerioden.gyldigTom().orElse(NO_END_DATE),
                            orgenhetService.getById(OTSomMatcherAgressoIdIPerioden.orgEnhetId()).get()
                        ));
                    lagErrorSomBrukerEksisterendeOTSomMathcerUpresisAgressoId(sspForNavident, stillingPeriode, OTSomMatcherAgressoIdIPerioden, navident);
                } else {
                    lagErrorKanIkkeMatcheStillingPeriodenTilEnhet(sspForNavident, stillingPeriode, navident);
                }
            }
        });

        // De StillingPeriodene med presis agressoId som vi ikke matchet over (1.iii.a)
        // må etterbehandles (for å sammenlignes meda alle de andre stillingsPeriodene)
        behandleUnmatchedPresisAgressoId(sspForNavident, unmatchedPresisAgressoId, matchetMedNOMenhet);

        sspForNavident.replaceStillingPerioder(slaaSammenBackToBackPerioderOgSorterAsc(matchetMedNOMenhet, sspForNavident));
    }

    public List<IOrgenhet> finnNomEnheterForEksaktIdIPerioden(String agressoId, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgenhetService.exactMatchByAgressoId(agressoId).stream()
            .filter(oe ->
                oe.erORGENHETnode() && oe.overlapper(gyldigFom, gyldigTom)
            )
            .toList();
    }

    private StillingPeriode behandlePresisAgressoIdMedSingleMatchPaaNOMEnhet(
        StillingPeriode stillingPeriode, IOrgenhet matchetOrgenhet, StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        if (erOrgenhetGyldigIHeleStillingPerioden(stillingPeriode, matchetOrgenhet)) {
            return new StillingPeriode(stillingPeriode, matchetOrgenhet);
        }

        // StillingsPerioden går utover funnet Orgenhets gyldighetsperiode - Må varsle og evt matche med eksisterende OT
        var eksisterendeOT = finnOTerSomRepresentererEnhetOgOverlapperPeriode(
            sspForNavident.navident(),
            stillingPeriode.gyldigFom(),
            stillingPeriode.gyldigTom(),
            matchetOrgenhet.getId()
        );

        if (!eksisterendeOT.isEmpty()) {
            // Ref: 'Matche til enheter i NOM'  1.i.a
            // Har en eller flere eksisterende OT som dekker inn i perioden for matchet Orgenhet.
            // Kan bruke kombinasjon av disse for å dekke perioder utenfor Enhetens gyldighetsperiode
            var nyGyldigFomTom = getNyGyldigFomTomAvOverlappendeOT(stillingPeriode, matchetOrgenhet, eksisterendeOT);

            lagErrorStillingPeriodeUtoverEnhetPeriodeMenMatcherEksisterendeOT(sspForNavident, stillingPeriode, sspForNavident.navident(), matchetOrgenhet);
            return new StillingPeriode(stillingPeriode.stilling(), nyGyldigFomTom.fom(), nyGyldigFomTom.tom(), matchetOrgenhet);
        } else {
            // Ref: 'Matche til enheter i NOM'  1.i.b
            // Har ikke eksisternede OT for StillingPerioden - Må endre StillingPerioden til å være innenfor Enhetens gyldighetsperiode
            LocalDate nyGyldigFom = getLatestDate(stillingPeriode.gyldigFom(), matchetOrgenhet.getGyldigFom());
            LocalDate nyGyldigTom = getEarliestDate(stillingPeriode.gyldigTom(), matchetOrgenhet.getGyldigTom());

            lagErrorStillingPeriodeUtoverEnhetPeriodeUtenMatchendeEksisterendeOT(
                sspForNavident, stillingPeriode, sspForNavident.navident(), matchetOrgenhet, nyGyldigFom, nyGyldigTom
            );
            return new StillingPeriode(stillingPeriode.stilling(), nyGyldigFom, nyGyldigTom, matchetOrgenhet);
        }
    }

    private NyGyldigFomTom getNyGyldigFomTomAvOverlappendeOT(StillingPeriode stillingPeriode, IOrgenhet orgenhet, List<OrgtilknytningState> eksisterendeOT) {
        LocalDate minOTGyldigFom = eksisterendeOT.stream().map(OrgtilknytningState::gyldigFom).min(LocalDate::compareTo).get();
        LocalDate maxOTGyldigTom = eksisterendeOT.stream().map(ot -> ot.gyldigTom().orElse(NO_END_DATE)).max(LocalDate::compareTo).get();

        LocalDate nyGyldigFom = getLatestDate(
            getEarliestDate(minOTGyldigFom, orgenhet.getGyldigFom()),
            stillingPeriode.gyldigFom()
        );
        LocalDate nyGyldigTom = getEarliestDate(
            getLatestDate(maxOTGyldigTom, orgenhet.getGyldigTom().orElse(NO_END_DATE)),
            stillingPeriode.gyldigTom()
        );

        NyGyldigFomTom result = new NyGyldigFomTom(nyGyldigFom, nyGyldigTom);
        return result;
    }

    private record NyGyldigFomTom(LocalDate fom, LocalDate tom) {}

    private List<StillingPeriode> behandlePresisAgressoIdMedMulitMatchPaaNOMEnhet(
        StillingPeriode stillingPeriode, List<IOrgenhet> orgenheter, StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        // Ref: 'Matche til enheter i NOM'  1.ii - Multi match på NOM enheter

        // Vi må splitte denne stillingperioden i flere perioder for å matche perioden til disse enhetene.
        // På samme måte som ved singel match må vi advare om vi ikke klarer å representere hele stillingsperioden i NOM, og evt benytte eksisterende OT.
        // Dette må logges/advares - og noe må korrigeres ett sted
        // Hvis noen av disse enhetene er overlappende i perioden, så må vi også advare om dette.
        // Splitten av StillingsPeriode gjøres på gyldigFOM datoen til den nyeste enheten (seneste gyldigFom)

        List<StillingPeriode> splittetPerioder = new ArrayList<>();

        LocalDate aktuellGyldigTom = stillingPeriode.gyldigTom();

        // Match perioder baklengs - altså bruk orgenheten som har senest gyldigFom først
        for(IOrgenhet oe: sorterMedNyesteEnhetFoerst(orgenheter)) {
            if (likEllerFoer(stillingPeriode.gyldigFom(),aktuellGyldigTom)) { // Vi har ikke 'fylt' opp perioden enda
                if (foer(oe.getGyldigTom().orElse(NO_END_DATE), aktuellGyldigTom)) {
                    // - Udekket periode mellom oe.getGyldigTom() og aktuellGyldigTom
                    // Sjekk om dette kan dekkes av eksisterende OT for denne enheten
                    var eksisterendeOT = finnOTerSomRepresentererEnhetOgOverlapperPeriode(
                        sspForNavident.navident(),
                        getLatestDate(oe.getGyldigFom(), stillingPeriode.gyldigFom()),
                        aktuellGyldigTom,
                        oe.getId()
                    );
                    if (!eksisterendeOT.isEmpty()) {
                        // Har eksisterende OT som dekker perioden for matchet Orgenhet, som enheten selv ikke er gyldig for
                        // Benytt den perioden for å dekke hullet, men advar om inkosistensen med orgenhetens gyldighetsperiode

                        // Har en eller flere eksisterende OT som dekker inn i perioden for matchet Orgenhet.
                        // Kan bruke kombinasjon av disse for å dekke perioder utenfor Enhetens gyldighetsperiode
                        var nyStillingsPeriode = dekkDelperiodeMedEksisterendeOTer(stillingPeriode, sspForNavident, aktuellGyldigTom, oe, eksisterendeOT);
                        splittetPerioder.add(nyStillingsPeriode);
                    } else {
                        // Har ikke eksisterende OT for StillingPerioden - Må endre StillingPerioden til å være innenfor Enhetens gyldighetsperiode
                        LocalDate nyGyldigFom = getLatestDate(stillingPeriode.gyldigFom(), oe.getGyldigFom());
                        LocalDate nyGyldigTom = getEarliestDate(aktuellGyldigTom, oe.getGyldigTom());

                        var nyStillingsPeriode = new StillingPeriode(
                            stillingPeriode.stilling(),
                            nyGyldigFom,
                            nyGyldigTom,
                            oe
                        );

                        lagErrorForDelAvStillingPeriodeSomGaarUtoverEnhetPeriodeUtenMatchendeEksisterendeOT(
                            nyGyldigFom, aktuellGyldigTom,
                            sspForNavident, stillingPeriode, sspForNavident.navident(), oe,
                            nyGyldigFom, nyGyldigTom,
                            dayAfter(nyGyldigTom), aktuellGyldigTom
                        );

                        splittetPerioder.add(nyStillingsPeriode);
                    }
                } else {
                    LocalDate nyGyldigFom = getLatestDate(stillingPeriode.gyldigFom(), oe.getGyldigFom());
                    // Matchet orgenhet kan dekke delperioden
                    var nyStillingsPeriode = new StillingPeriode(
                        stillingPeriode.stilling(),
                        nyGyldigFom,
                        aktuellGyldigTom,
                        oe
                    );
                    splittetPerioder.add(nyStillingsPeriode);
                }
            }
            if (etter(getEarliestDate(stillingPeriode.gyldigTom(), oe.getGyldigTom().orElse(NO_END_DATE)), aktuellGyldigTom)) {
                // Vi har her en orgenhet med gyldigTom som overlapper inn i den forrige orgenhetens periode.
                // Det vil si at perioden med overlapp faktisk kunne tilhørt begge enhetene
                // Advar om dette
                lagErrorForDelAvStillingPeriodeSomHvorMatchetEnhetSinPeriodeGaarInnIForrigeBehandledeDelPeriode(
                    aktuellGyldigTom, oe.getGyldigTom().orElse(NO_END_DATE),
                    sspForNavident, stillingPeriode, sspForNavident.navident(), oe
                );
            }
            aktuellGyldigTom = dayBefore(splittetPerioder.getLast().gyldigFom());
        }

        if (stillingPeriode.gyldigFom().isBefore(aktuellGyldigTom)) {
            // Enhetperiodene dekker ikke starten av perioden til StillingPerioden

            // Sjekk om dette kan dekkes av eksisterende OT for denne enheten
            IOrgenhet sisteEnhet = sorterMedNyesteEnhetFoerst(orgenheter).getLast();

            var eksisterendeOT = finnOTerSomRepresentererEnhetOgOverlapperPeriode(
                sspForNavident.navident(),
                stillingPeriode.gyldigFom(),
                aktuellGyldigTom,
                sisteEnhet.getId()
            );

            if (!eksisterendeOT.isEmpty()) {
                // Har eksisterende OT som dekker perioden for matchet Orgenhet, som enheten selv ikke er gyldig for
                // Benytt den perioden for å dekke hullet, men advar om inkosistensen med orgenhetens gyldighetsperiode

                // Har en eller flere eksisterende OT som dekker inn i perioden for matchet Orgenhet.
                // Kan bruke kombinasjon av disse for å dekke perioder utenfor Enhetens gyldighetsperiode
                var nyStillingsPeriode = dekkDelperiodeMedEksisterendeOTer(stillingPeriode, sspForNavident, aktuellGyldigTom, sisteEnhet, eksisterendeOT);
                splittetPerioder.add(nyStillingsPeriode);
                aktuellGyldigTom = dayBefore(nyStillingsPeriode.gyldigFom());
            }
            if (stillingPeriode.gyldigFom().isBefore(aktuellGyldigTom)) {
                lagErrorForDelAvStillingPeriodeSomGaarUtoverEnhetPeriodeUtenMatchendeEksisterendeOT(
                    stillingPeriode.gyldigFom(), splittetPerioder.getLast().gyldigTom(),
                    sspForNavident, stillingPeriode, sspForNavident.navident(),
                    sisteEnhet,
                    splittetPerioder.getLast().gyldigFom(), splittetPerioder.getLast().gyldigTom(),
                    stillingPeriode.gyldigFom(), dayBefore(splittetPerioder.getLast().gyldigFom())
                );
            }
        }
        return slaaSammenBackToBackPerioderOgSorterAsc(splittetPerioder, sspForNavident);
    }

    private StillingPeriode dekkDelperiodeMedEksisterendeOTer(
        StillingPeriode stillingPeriode,
        StrukturerteStillingPerioderForNavident sspForNavident,
        LocalDate aktuellGyldigTom,
        IOrgenhet sisteEnhet,
        List<OrgtilknytningState> eksisterendeOT
    ) {
        var nyGyldigFomTom = getNyGyldigFomTomAvOverlappendeOT(stillingPeriode, sisteEnhet, eksisterendeOT);

        var nyStillingsPeriode = new StillingPeriode(
            stillingPeriode.stilling(),
            nyGyldigFomTom.fom(),
            nyGyldigFomTom.tom(),
            sisteEnhet
        );

        lagErrorForDelAvStillingPeriodeSomGaarUtoverEnhetPeriodeMenMatcherEksisterendeOT(
            nyStillingsPeriode.gyldigFom(), nyStillingsPeriode.gyldigTom(),
            sspForNavident, stillingPeriode, sspForNavident.navident(), sisteEnhet
        );

        return nyStillingsPeriode;
    }

    private static List<IOrgenhet> sorterMedNyesteEnhetFoerst(List<IOrgenhet> orgenheter) {
        return orgenheter.stream()
            .sorted(Comparator.comparing((IOrgenhet orgenhet) -> orgenhet.getGyldigFom() == null ? NO_END_DATE : orgenhet.getGyldigFom()).reversed()).toList();
    }

    private void behandleUnmatchedPresisAgressoId(
        StrukturerteStillingPerioderForNavident sspForNavident,
        TreeMap<Integer, StillingPeriode> unmatchedPresisAgressoId,
        List<StillingPeriode> matchetMedNOMEnhet
    ) {
        unmatchedPresisAgressoId.reversed().forEach((plassering, unmatched) -> {
            // Hvis det eksisterer en OT på ressursen for samme periode og det er ikke flere andre StillingPerioder på samme periode,
            // så forvent at den eksisterende er/var korrekt, og benytt den som enhet i StillingPerioden for å forhindre uønsket
            // fjerning/endring av OT som antagelig er korrekt

            if (!dekkerEksisterendeStillingPerioderAngittStillingPeriode(matchetMedNOMEnhet, unmatched)) {
                // Mangler eksisterende Stillingperioder i denne perioden
                // Se om vi kan gjenbruke eksisterende OT for perioden

                var eksisterendeOT = finnOTSomRepresentererEksaktPeriode(
                    sspForNavident.navident(),
                    unmatched.gyldigFom(),
                    unmatched.gyldigTom()
                );

                if (!eksisterendeOT.isEmpty()) {
                    // Har eksisterende OT(er) som dekker perioden for matchet Orgenhet.
                    String orgenhetId = null;
                    if (eksisterendeOT.size() == 1) {
                        orgenhetId = eksisterendeOT.getFirst().orgEnhetId();
                    } else {
                        // Er det flere - ta den som evt er DOL, eller bare plukk den første i listen
                        var dolOT = eksisterendeOT.stream().filter(OrgtilknytningState::erDagligOppfolging).findFirst().orElse(null);
                        orgenhetId = dolOT != null ? dolOT.orgEnhetId() : eksisterendeOT.getFirst().orgEnhetId();
                    }
                    // Kan bruke denne for StillingPerioden

                    var nyStillingPeriode = new StillingPeriode(unmatched, orgenhetService.getById(orgenhetId).get());

                    // Stikk denne nye stillingPeriode inn i plasseringen på matchetMedNOMEnheter - slik at rekkefølgen beholdes
                    // Innstikk plassering idx ivaretas siden vi løper igjennom unmatched i reversert rekkefølge
                    matchetMedNOMEnhet.add(plassering, nyStillingPeriode);

                    lagErrorFinnerIkkeEnhetForPresisAgressoIdOgHarIngenAndreStillingperioderMenViHarEksisterendeOT(
                        sspForNavident, unmatched, sspForNavident.navident(), orgenhetId
                    );
                } else {
                    lagErrorHarPresisAgressoIdMenKlarerIkkeFinneEnhetAABrukeForStillingPerioden(sspForNavident, unmatched, sspForNavident.navident());
                }
            } else {
                // Vi bruker ikke denne StillingsPerioden - ressursen har perioden dekt allerede - kun varsle
                lagErrorFinnerIkkeEnhetForPresisAgressoIdMenPeriodenErDekt(sspForNavident, unmatched, sspForNavident.navident());
            }
        });
    }

    protected boolean dekkerEksisterendeStillingPerioderAngittStillingPeriode(List<StillingPeriode> eksisterende, StillingPeriode stillingPeriode) {
        // finn alle eksisterende som er i perioden
        var eksisterendeIPerioden = eksisterende.stream()
            .filter( eksisterendeSp -> overlapsInterval(eksisterendeSp, stillingPeriode))
            .sorted(Comparator.comparing(StillingPeriode::gyldigFom))
            .toList();

        LocalDate forrigeGyldigTom = null;
        for (var eksisterendeSp : eksisterendeIPerioden) {
            if (forrigeGyldigTom != null) {
                if (forrigeGyldigTom.isBefore(stillingPeriode.gyldigTom()) &&
                    !overlapperEllerErDirekteEtterfoelgende(forrigeGyldigTom, eksisterendeSp.gyldigFom())
                ) {
                    // Det er hull i perioden
                    return false;
                }
            } else if (!likEllerFoer(eksisterendeSp.gyldigFom(), stillingPeriode.gyldigFom())) {
                // Første eksisterende etter periodens start
                return false;
            }
            forrigeGyldigTom = eksisterendeSp.gyldigTom();
        };
        if (forrigeGyldigTom == null || forrigeGyldigTom.isBefore(stillingPeriode.gyldigTom())) {
            // Dekker ikke slutten av perioden
            return false;
        }
        return true;
    }

    private boolean overlapperEllerErDirekteEtterfoelgende(LocalDate forrigeTom, LocalDate nesteFom) {
        return likEllerFoer(nesteFom, dayAfter(forrigeTom));
    }

    private LocalDate getLatestDate(LocalDate d1, LocalDate d2) {
        d1 = d1 == null ? NO_END_DATE : d1;
        return d1.isBefore(d2) ? d2 : d1;
    }

    private LocalDate getEarliestDate(LocalDate d1, LocalDate d2) {
        d1 = d1 == null ? NO_END_DATE : d1;
        return d1.isBefore(d2) ? d1 : d2;
    }

    private LocalDate getEarliestDate(LocalDate d1, Optional<LocalDate> d2) {
        d1 = d1 == null ? NO_END_DATE : d1;
        return getEarliestDate(d1, d2.orElse(NO_END_DATE));
    }

    private boolean erOrgenhetGyldigIHeleStillingPerioden(StillingPeriode stillingPeriode, IOrgenhet matchetOrgenhet) {
        return likEllerEtter(stillingPeriode.gyldigFom(), matchetOrgenhet.getGyldigFom())
            && likEllerFoer(stillingPeriode.gyldigTom(), matchetOrgenhet.getGyldigTom().orElse(NO_END_DATE));
    }

    private List<OrgtilknytningState> finnOTerSomRepresentererEnhetOgOverlapperPeriode(String navident, LocalDate gyldigFom, LocalDate gyldigTom, String orgenhetId) {
        return orgtilknytningStateRepository.findByNavident(navident).stream()
            .filter(ot ->
                ot.orgEnhetId().equals(orgenhetId) &&
                    overlapsInterval(gyldigFom, gyldigTom, ot)
            )
            .toList();
    }

    private OrgtilknytningState finnOTSomMatcherUpresisAgressoIdOgDekkerPerioden(
        String navident, LocalDate gyldigFom, LocalDate gyldigTom, String upresisAgressoId
    ) {
        var matchedeOrgenheter = orgenhetService.matchByAgressoId(upresisAgressoId);
        if (matchedeOrgenheter.isEmpty()) {
            return null;
        }
        var OTer = orgtilknytningStateRepository.findByNavident(navident).stream()
            .filter(ot ->
                matchedeOrgenheter.stream().map(IOrgenhet::getId).anyMatch(orgenhetId -> orgenhetId.equals(ot.orgEnhetId())) &&
                    overlapsInterval(gyldigFom, gyldigTom, ot)
            )
            .toList();

        var bestMatch = getBestMatchPaaPerioden(OTer, gyldigFom, gyldigTom);

        return bestMatch;
    }

    private OrgtilknytningState getBestMatchPaaPerioden(List<OrgtilknytningState> OTer, LocalDate gyldigFom, LocalDate gyldigTom) {
        // finn evt presise matcher på periode
        var presisMatch = OTer.stream().filter(ot -> lik(gyldigFom, ot.gyldigFom()) && lik(gyldigTom, ot.gyldigTom().orElse(null))).toList();
        if (presisMatch.size() > 0) {
            OTer = presisMatch;
        } else {
            // Hvis ingen finn evt noen som er 'current'
            var current = OTer.stream().filter(PeriodeUtil::isCurrentPeriod).toList();
            if (current.size() > 0) {
                OTer = current;
            }
        }
        // Hvis ikke bruk alle

        // Av disse bruk bare DOL perioder hvis de finnes
        if (OTer.stream().anyMatch(OrgtilknytningState::erDagligOppfolging)) {
            OTer = OTer.stream().filter(OrgtilknytningState::erDagligOppfolging).toList();
        }
        // Og av disse igjen, bare plukk den første
        return OTer.stream().findFirst().orElse(null);
    }

    private List<OrgtilknytningState> finnOTSomRepresentererEksaktPeriode(String navident, LocalDate gyldigFom, LocalDate gyldigTom) {
        return orgtilknytningStateRepository.findByNavident(navident).stream()
            .filter(ot ->
                ot.gyldigFom().equals(gyldigFom) &&
                ot.gyldigTom().orElse(NO_END_DATE).equals(gyldigTom)
            )
            .toList();
    }

    protected List<StillingPeriode> slaaSammenBackToBackPerioderOgSorterAsc(List<StillingPeriode> stillingPerioder,
                                                                            StrukturerteStillingPerioderForNavident sspForNavident
    ) {
        stillingPerioder = advarOgFjernNegativePerioder(stillingPerioder, sspForNavident);

        List<StillingPeriode> sammenslaatt = new ArrayList<>();
        var gruppertOrgenhetOgDol = stillingPerioder.stream().collect(groupingBy(sp -> sp.orgenhet().getId() + " - " + sp.dagligOppfolging()));

        gruppertOrgenhetOgDol.forEach((OrgenhetOgDol, stillingPerioderForOrgenhetOgDol) -> {
            stillingPerioderForOrgenhetOgDol.sort(Comparator.comparing(StillingPeriode::gyldigFom));
            StillingPeriode forrige = null;
            for (StillingPeriode sp: stillingPerioderForOrgenhetOgDol) {
                if (forrige == null) {
                    forrige = sp;
                } else {
                    if (likEllerFoer(sp.gyldigFom(), dayAfter(forrige.gyldigTom()))) {
                        forrige = new StillingPeriode(forrige.stilling(), forrige.gyldigFom(), sp.gyldigTom(), forrige.orgenhet(), forrige.dagligOppfolging());
                    } else {
                        sammenslaatt.add(forrige);
                        forrige = sp;
                    }
                }
            }
            if (forrige != null) {
                sammenslaatt.add(forrige);
            }
        });

        sammenslaatt.sort(
            Comparator.comparing(StillingPeriode::gyldigFom)
                .thenComparing(StillingPeriode::dagligOppfolging)
                .thenComparing(sp -> sp.orgenhet().getId()));

        return sammenslaatt;
    }

    private List<StillingPeriode> advarOgFjernNegativePerioder(List<StillingPeriode> stillingPerioder, StrukturerteStillingPerioderForNavident sspForNavident) {
        stillingPerioder.stream()
            .filter(sp -> foer(sp.gyldigTom(), sp.gyldigFom()))
            .forEach(sp -> {
                lagErrorHarNegativStillingPeriode(sspForNavident, sp, sspForNavident.navident());
            });

        return stillingPerioder.stream()
            .filter(sp -> likEllerEtter(sp.gyldigTom(), sp.gyldigFom()))
            .toList();
    }


    private void lagErrorStillingPeriodeUtoverEnhetPeriodeMenMatcherEksisterendeOT(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        IOrgenhet orgenhet
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover enhetens (%s %s) gyldighetsperiode [%s - %s].
            Men det finnes allerede eksisterende Orgtilknytning(er) for angitt periode, så vi beholder denne og utvider evt StillingPerioden
            basert på OT'enes perioder som går utover enhetens gyldighetsperiode.
            """
                .formatted(navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    orgenhet.getId(), orgenhet.getNavn(),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().orElse(null)),
            stillingPeriode
        );
    }

    private void lagErrorStillingPeriodeUtoverEnhetPeriodeUtenMatchendeEksisterendeOT(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        IOrgenhet orgenhet,
        LocalDate nyGyldigFom,
        LocalDate nyGyldigTom
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_PERIODE,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover enhetens (%s %s) gyldighetsperiode [%s - %s].
            Det finnes ingen eksisterende Orgtilknytning for angitt periode, så vi må endre StillingPerioden til å være innenfor enhetens gyldighetsperiode.
            Ny gyldighetsperiode for StillingPerioden blir da [%s - %s].
            """
                .formatted(navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    orgenhet.getId(), orgenhet.getNavn(),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().orElse(null),
                    nyGyldigFom, nyGyldigTom),
            stillingPeriode
        );
    }

    private void lagErrorForDelAvStillingPeriodeSomGaarUtoverEnhetPeriodeMenMatcherEksisterendeOT(
        LocalDate delperiodeGyldigFom, LocalDate delperiodeGyldigTom,
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        IOrgenhet orgenhet
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__BRUKER_EKSISTERENDE_OT,
            """
            Delperiode [%s - %s] av StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover den
            matchet enhetens (%s %s) gyldighetsperiode [%s - %s].
            Men det finnes allerede eksisterende Orgtilknytning(er) for angitt periode, så vi beholder denne og utvider evt StillingPerioden
            basert på OT'enes perioder som går utover enhetens gyldighetsperiode.
            """
                .formatted(
                    delperiodeGyldigFom, delperiodeGyldigTom,
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    orgenhet.getId(), orgenhet.getNavn(),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().orElse(null)),
            stillingPeriode
        );
    }

    private void lagErrorForDelAvStillingPeriodeSomGaarUtoverEnhetPeriodeUtenMatchendeEksisterendeOT(
        LocalDate delperiodeGyldigFom, LocalDate delperiodeGyldigTom,
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        IOrgenhet orgenhet,
        LocalDate nyGyldigFom, LocalDate nyGyldigTom,
        LocalDate hullFom, LocalDate hullTom
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.DEL_AV_STILLINGSPERIODE_UTENFOR_ORGENHET_GYLDIGHET__ENDRER_DELPERIODE,
            """
            Delperiode [%s - %s] av StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] går utover den
            matchet enhetens (%s %s) gyldighetsperiode [%s - %s].
            Det finnes ingen eksisterende Orgtilknytning for angitt periode, så vi må endre delperioden til å være innenfor enhetens gyldighetsperiode.
            Ny gyldighetsperiode for delperioden blir da [%s - %s].
            Dette skaper ett hull i Orgtilknytningene i perioden [%s - %s]
            """
                .formatted(
                    delperiodeGyldigFom, delperiodeGyldigTom,
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    orgenhet.getId(), orgenhet.getNavn(),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().orElse(null),
                    nyGyldigFom, nyGyldigTom,
                    hullFom, hullTom
                ),
            stillingPeriode
        );
    }

    private void lagErrorForDelAvStillingPeriodeSomHvorMatchetEnhetSinPeriodeGaarInnIForrigeBehandledeDelPeriode(
        LocalDate delperiodeGyldigFom, LocalDate delperiodeGyldigTom,
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        IOrgenhet orgenhet
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.FLERE_ENHETER_KAN_MATCHES_FOR_DEL_AV_STILLINGSPERIODE,
            """
            Delperiode [%s - %s] av StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s]
            matchet enheten (%s %s) med gyldighetsperiode [%s - %s].
            Denne enheten har gyldighetsperiode som overlapper inn i neste delperiode for denne splittede StillingsPerioden.
            Det vil si at det faktisk er to eller flere enheter som kan matche denne overlapp-perioden [%s - %s].
            Enheten som er valgt, er den som er nyest (yngste gyldigFom)
            """
                .formatted(
                    delperiodeGyldigFom, delperiodeGyldigTom,
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    orgenhet.getId(), orgenhet.getNavn(),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().orElse(null),
                    dayAfter(delperiodeGyldigTom), getEarliestDate(stillingPeriode.gyldigTom(), orgenhet.getGyldigTom())
                ),
            stillingPeriode
        );
    }

    private void lagErrorFinnerIkkeEnhetForPresisAgressoIdMenPeriodenErDekt(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MEN_PERIDOE_ER_DEKT,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] og AgressoId %s har ingen matchende enhet i NOM.
            Men det finnes annen Stillingsperiode som dekker denne perioden, så vi velger å ikke benytte denne.
            """
                .formatted(
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.getPresisAgressoId()
                ),
            stillingPeriode
        );
    }

    private void lagErrorFinnerIkkeEnhetForPresisAgressoIdOgHarIngenAndreStillingperioderMenViHarEksisterendeOT(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident,
        String orgenhetId
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_MATCHER__MOT_EKSISTERENDE_OT,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] og AgressoId %s har ingen matchende enhet i NOM.
            Det finnes heller ingen annen Stillingsperiode som dekker denne perioden, men vi har eksisterende OT for enhet %s som vi benytter i steden.
            """
                .formatted(
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.getPresisAgressoId(),
                    orgenhetId
                ),
            stillingPeriode
        );
    }

    private void lagErrorHarPresisAgressoIdMenKlarerIkkeFinneEnhetAABrukeForStillingPerioden(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.FINNER_IKKE_ENHET_FOR_PRESIS_AGRESSOID_OG_HAR_INGEN_ANDRE_STILLINGSPERIODER_ELLER_ORGTILKNYTNINGER,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] og AgressoId %s har ingen matchende enhet i NOM.
            Det finnes heller ingen annen Stillingsperiode som dekker denne perioden eller eksisterende orgtilknytninger i perioden.
            Dette medfører at vi får 'hull' i orgtilknytningene, selv om det kan se ut til at DVH/Unit4 mener det ikke skal være det.
            """
                .formatted(
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.getPresisAgressoId()
                ),
            stillingPeriode
        );
    }

    private void lagErrorSomBrukerEksisterendeOTSomMathcerUpresisAgressoId(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        OrgtilknytningState otSomMatcherAgressoIdIPerioden,
        String navident
    ) {
        var oe = orgenhetService.getById(otSomMatcherAgressoIdIPerioden.orgEnhetId()).get();
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.AGRESSOID_PAA_STILLING_ER_IKKE_PRESIS_MEN_BRUKER_EKSISTERENDE_OT_SOM_MATCHER,
            """
            StillingsPeriode for Navident %s med gyldighetsperiode [%s - %s] har upresis AgressoId (%s).
            Det finnes eksisterende orgtilknytning for enhet [%s (%s) %s] med periode [%s - %s] som dekker StillingPerioden.
            Vi benytter enhet og periode for den opprinnelige orgtilknytningen for å sikre at denne ikke fjernes.
            Det kan gi en annen periode på StillingsPerioden enn det er på DvhRessursStillingene som ligger til grunn.
            """
                .formatted(
                    navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.stilling().agressoId(),
                    oe.getId(), oe.getNavn(), oe.getAgressoId(),
                    otSomMatcherAgressoIdIPerioden.gyldigFom(), otSomMatcherAgressoIdIPerioden.gyldigTom().orElse(null)
                ),
            stillingPeriode
        );
    }

    private void lagErrorKanIkkeMatcheStillingPeriodenTilEnhet(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.KAN_IKKE_MATCHE_STILLINGSPERIODE_TIL_ENHET,
            """
            Kan ikke matche StillingPerioden for Navident %s med gyldighetsperiode [%s - %s]
            med AgressoId på Stilling (%s) og med Orgenhet På Ressurs (%s) til noen enhet i NOM.
            """
                .formatted(navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.stilling().agressoId(), stillingPeriode.stilling().ressursAgressoId()),
            stillingPeriode
        );
    }

    private void lagErrorHarNegativStillingPeriode(
        StrukturerteStillingPerioderForNavident sspForNavident,
        StillingPeriode stillingPeriode,
        String navident
    ) {
        sspForNavident.addError(
            StrukturerteStillingPerioderForNavident.ErrorType.HAR_LAGET_NEGATIV_STILLINGPERIODE__DENNE_FJERNES,
            """
            Har på en eller annen måte laget negativ StillingPeriode for Navident %s med gyldighetsperiode [%s - %s] for enhet [%s (%s) %s]
            Denne fjernes
            """
                .formatted(navident,
                    stillingPeriode.gyldigFom(), stillingPeriode.gyldigTom(),
                    stillingPeriode.orgenhet().getId(), stillingPeriode.orgenhet().getNavn(), stillingPeriode.orgenhet().getAgressoId()),
            stillingPeriode
        );
    }

    protected void settKorrekteDOLPerioder(StrukturerteStillingPerioderForNavident sspForNavident) {
    }
}

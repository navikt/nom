package no.nav.nom.apps.api.statistikk.api;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodActive;
import static no.nav.nom.apps.api.common.SpringCacheConfig.STATISTIKK_REST_SERVICE_CACHE_NAME;

@Slf4j
@Service
public class StatistikkRestService {

    private final RessursStateRepository ressursStateRepository;

    private final OrgenhetService orgenhetService;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgEnhetMapper orgEnhetMapper;

    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateOrganiseringListener nomStateOrganiseringListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public StatistikkRestService(
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService,
        RemedyEnhetMapperService remedyEnhetMapperService,
        OrgEnhetMapper orgEnhetMapper,
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateOrganiseringListener nomStateOrganiseringListener,
        NomStateOrgEnhetListener nomStateOrgEnhetListener,
        OrgtilknytningStateRepository orgtilknytningStateRepository
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateOrganiseringListener = nomStateOrganiseringListener;
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public GlobalStatistikkResponse hentGlobalStatistikk() {
        var alleRessurser = ressursStateRepository.findAllActiveRessurser().values().stream()
            .distinct()
            .filter(ressursState -> ressursState.startdato() != null && !ressursState.startdato().isAfter(LocalDate.now()))
            .filter(this::filterGyldigeRessurs)
            .toList();
        var ressursStatistikk = new RessursStatistikk(
            alleRessurser.size(),
            alleRessurser.stream().filter(RessursState::isStatlig).count(),
            alleRessurser.stream().filter(RessursState::isKommunal).count(),
            alleRessurser.stream().filter(RessursState::isEkstern).count()
        );

        var alleOrgEnheter = orgenhetService.getAktiveEnheterIOrganisasjonen().stream().toList();
        var antallLovNoder = orgenhetService.getAllOrgEnheterThatCanHaveRessurser().size();

        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER);
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING);
        var antallKlageinstanser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.KLAGEINSTANS);
        var antallNavKontaktsenter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_KONTAKTSENTER);
        var antallKontrollKonrollenheter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.KONTROLL_KONTROLLENHET);
        var antallFylker = createNewIOrgenhetList(alleOrgEnheter, NomNivaa.DRIFTSENHET, OrgEnhetsType.FYLKE).size();
        int antallNavKontorer = createNewIOrgenhetList(alleOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.NAV_KONTOR).size();

        return new GlobalStatistikkResponse(
            ressursStatistikk,
            antallNavArbeidOgYtelser,
            antallLovNoder,
            antallNavFamilieOgPensjonsytelser,
            antallHjelpemidlerOgTilrettelegging,
            antallKlageinstanser,
            antallNavKontaktsenter,
            antallKontrollKonrollenheter,
            antallFylker,
            antallNavKontorer
        );
    }


    public void loadStatistikkCache() {
        log.info("Loading statistikkRestService");
        while (!isInitialStateReady()) {
            log.info("Waiting for NomState listeners to be ready before loading statistikk cache");
            try {
                //noinspection BusyWait
                Thread.sleep(10000L); // Wait for 10 seconds before checking again
            } catch (InterruptedException e) {
                log.error("Thread interrupted while waiting for NomState listeners to be ready", e);
                Thread.currentThread().interrupt();
            }
        }
        var alleOrgEnheterSomErLinjeEllerDriftId = orgenhetService.getAktiveEnheterIOrganisasjonen()
            .stream()
            .filter(iOrgenhet -> nonNull(iOrgenhet.getNomNivaa()) &&
                (iOrgenhet.getNomNivaa().equals(NomNivaa.LINJEENHET) || iOrgenhet.getNomNivaa().equals(NomNivaa.DRIFTSENHET)))
            .map(IOrgenhet::getId)
            .toList();
        alleOrgEnheterSomErLinjeEllerDriftId.forEach(this::getStatistikkForOrgEnhetCache);
    }

    private OrgEnhetStatistikkResponse hentStatistikkForOrgEnhet(String orgEnhetId) {
        var optionalOrgEnhet = orgenhetService.getById(orgEnhetId);
        if (optionalOrgEnhet.isEmpty()) {
            throw new OrgEnhetNotFoundException("Fant ikke orgenhet med id " + orgEnhetId);
        }

        var orgEnheter = new ArrayList<>(orgenhetService.getAllUnderliggendeOrgEnhetById(optionalOrgEnhet.get().getId()));
        orgEnheter.add(optionalOrgEnhet.get());

        var alleRelevanteOrgtilknytningerByNavident = orgEnheter.stream()
            .map(orgEnhetMapper::mapToOrgEnhet)
            .map(OrgEnhet::ressursKoblinger)
            .flatMap(Collection::stream)
            .filter(this::erRelevantOt)
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        var sektorCounts = new HashMap<>(Map.of(
                Sektor.NAV_STATLIG, new ArrayList<String>(),
                Sektor.NAV_KOMMUNAL, new ArrayList<String>(),
                Sektor.EKSTERN, new ArrayList<String>()
        ));

        for(var navident : alleRelevanteOrgtilknytningerByNavident.keySet()) {
            var otList = alleRelevanteOrgtilknytningerByNavident.get(navident);

            var relevantDolOtList = otList.stream().filter(OrgtilknytningState::erDagligOppfolging).toList();
            if(relevantDolOtList.size() == 1) {
                var otSektor = Optional.ofNullable(relevantDolOtList.getFirst().sektor())
                    .orElseGet(() -> ressursStateRepository.findByNavident(navident).map(RessursState::sektor).orElse(null));
                if (otSektor != null) {
                    sektorCounts.getOrDefault(otSektor, new ArrayList<>()).add(navident);
                } else {
                    log.warn("Klarte ikke bestemme sektor for navident " + navident);
                }
                continue;
            }
            if (relevantDolOtList.size() > 1) {
                log.info("Navident " + navident + " har flere enn 1 aktiv orgtilknytning med DOL == true");
                var maybeSektor = ressursStateRepository.findByNavident(navident)
                    .map(RessursState::sektor);
                maybeSektor.ifPresent(sektor -> sektorCounts.getOrDefault(sektor, new ArrayList<>()).add(navident));
                continue;
            }

            var distinctSectorList = otList.stream().map(OrgtilknytningState::sektor).filter(Objects::nonNull).distinct().toList();
            if (distinctSectorList.size() == 1) {
                sektorCounts.get(distinctSectorList.getFirst()).add(navident);
                continue;
            }

            // fallback, bruk DOL OT for denne ressursen, selv om gitt OT ligger utenfor relevant hierarki
            var maybDolOtGlobally = orgtilknytningStateRepository.findByNavident(navident)
                .stream()
                .filter(it -> isPeriodActive(it.gyldigFom(), it.gyldigFom()))
                .filter(OrgtilknytningState::erDagligOppfolging)
                .findFirst();
            if (maybDolOtGlobally.isPresent()) {
                var ot = maybDolOtGlobally.get();
                if (ot.sektor() != null) {
                    sektorCounts.get(ot.sektor()).add(navident);
                }
            } else {
                var maybeSektor = ressursStateRepository.findByNavident(navident).map(RessursState::sektor);

                maybeSektor.ifPresentOrElse(
                    sektor -> sektorCounts.get(sektor).add(navident),
                    () -> log.warn("Kunne ikke bestemme sektor for ressurs " + navident + " i kontekst av enhet " + orgEnhetId)
                );
            }
        }

        var ressursStatistikk = RessursStatistikk.builder()
            .antallAktiveRessurser(alleRelevanteOrgtilknytningerByNavident.size())
            .antallStatligeRessurser(sektorCounts.get(Sektor.NAV_STATLIG).size())
            .antallKommunaleRessurser(sektorCounts.get(Sektor.NAV_KOMMUNAL).size())
            .antallEksterneRessurser(sektorCounts.get(Sektor.EKSTERN).size())
            .build();

        { // verification
            var testSet = new HashSet<String>(alleRelevanteOrgtilknytningerByNavident.keySet());
            sektorCounts.get(Sektor.NAV_STATLIG).forEach(testSet::remove);
            sektorCounts.get(Sektor.NAV_KOMMUNAL).forEach(testSet::remove);
            sektorCounts.get(Sektor.EKSTERN).forEach(testSet::remove);
            if(!testSet.isEmpty()){
                var problamticNavIdentJoinedString = String.join(", ", testSet);
                log.warn("For orgenhet " + orgEnhetId +": Følgende navidenter mangler i sum av alle ressurser på enheten: " + problamticNavIdentJoinedString);
            }
        }

        var lovnoder = orgenhetService.getAllOrgEnheterThatCanHaveRessurser(optionalOrgEnhet.get().getId());
        var erNestNedersteNode = orgenhetService.isOrgEnhetBeforeLeafNode(optionalOrgEnhet.get().getId(), lovnoder);
        var antallLovnoder = erNestNedersteNode ? 0 : lovnoder.size();

        if (nonNull(optionalOrgEnhet.get().getNomNivaa())) {
            if (optionalOrgEnhet.get().getNomNivaa().equals(NomNivaa.LINJEENHET)) {
                return hentStatistikkForLinjeenhet(ressursStatistikk, antallLovnoder, orgEnheter);
            } else if (optionalOrgEnhet.get().getNomNivaa().equals(NomNivaa.DRIFTSENHET)) {
                return hentStatistikkForDriftsenhet(optionalOrgEnhet.get(), ressursStatistikk, antallLovnoder, orgEnheter);
            }
        }
        return new OrgEnhetStatistikkResponse(
            ressursStatistikk,
            antallLovnoder,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }

    private OrgEnhetStatistikkResponse hentStatistikkForLinjeenhet(
        RessursStatistikk ressursStatistikk,
        int antallLovnoder,
        Collection<IOrgenhet> underliggendeOrgEnheter
    ) {
        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(
            underliggendeOrgEnheter,
            OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER
        );
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(
            underliggendeOrgEnheter,
            OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING
        );
        var antallKlageinstanser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KLAGEINSTANS);
        var antallNavKontaktsenter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_KONTAKTSENTER);
        var antallKontrollKonrollenheter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KONTROLL_KONTROLLENHET);
        var antallFylker = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.DRIFTSENHET, OrgEnhetsType.FYLKE).size();
        var antallNavKontorer = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.NAV_KONTOR).size();
        var antallArbeidslivssenter = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.ARBEIDSLIVSSENTER).size();
        var antallArbeidsraadgivning = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.ARBEIDSRAADGIVNING).size();
        var antallTiltak = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.TILTAK).size();

        return new OrgEnhetStatistikkResponse(
            ressursStatistikk,
            antallLovnoder,
            antallNavArbeidOgYtelser,
            antallNavFamilieOgPensjonsytelser,
            antallHjelpemidlerOgTilrettelegging,
            antallKlageinstanser,
            antallNavKontaktsenter,
            antallKontrollKonrollenheter,
            antallFylker,
            antallNavKontorer,
            antallArbeidslivssenter,
            antallArbeidsraadgivning,
            antallTiltak
        );
    }

    private OrgEnhetStatistikkResponse hentStatistikkForDriftsenhet(
        IOrgenhet orgEnhet,
        RessursStatistikk ressursStatistikk,
        int antallLovnoder,
        Collection<IOrgenhet> underliggendeOrgEnheter
    ) {
        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(
            underliggendeOrgEnheter,
            OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER
        );
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(
            underliggendeOrgEnheter,
            OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING
        );
        var antallKlageinstanser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KLAGEINSTANS);
        var antallNavKontaktsenter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_KONTAKTSENTER);
        var antallKontrollKonrollenheter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KONTROLL_KONTROLLENHET);

        var antallFylker = 0;
        if (orgEnhet.getOrgEnhetsType() != OrgEnhetsType.FYLKE) {
            antallFylker = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.DRIFTSENHET, OrgEnhetsType.FYLKE).size();
        }

        var antallNavKontorer = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.NAV_KONTOR).size();

        return new OrgEnhetStatistikkResponse(
            ressursStatistikk,
            antallLovnoder,
            antallNavArbeidOgYtelser,
            antallNavFamilieOgPensjonsytelser,
            antallHjelpemidlerOgTilrettelegging,
            antallKlageinstanser,
            antallNavKontaktsenter,
            antallKontrollKonrollenheter,
            antallFylker,
            antallNavKontorer,
            0,
            0,
            0
        );
    }

    private int getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(Collection<IOrgenhet> orgenheter, OrgEnhetsType orgEnhetsType) {
        var driftenhetOrgEnhetsType = createNewIOrgenhetList(orgenheter, NomNivaa.DRIFTSENHET, orgEnhetsType);
        var arbeidsomraadeOrgEnhetsType = createNewIOrgenhetList(orgenheter, NomNivaa.ARBEIDSOMRAADE, orgEnhetsType);

        Set<RemedyEnhet> enheterToIgnore = new HashSet<>();
        driftenhetOrgEnhetsType.forEach(orgEnhet -> remedyEnhetMapperService.findByNomId(orgEnhet.getId()).ifPresent(enheterToIgnore::add));
        Set<RemedyEnhet> navRemedyEnhet = new HashSet<>();
        arbeidsomraadeOrgEnhetsType.forEach(orgEnhet -> remedyEnhetMapperService.findByNomId(orgEnhet.getId()).ifPresent(navRemedyEnhet::add));
        return navRemedyEnhet.stream()
            .filter(navKontor -> enheterToIgnore.stream()
                .noneMatch(fylkeEnhet -> fylkeEnhet.remedyEnhetId().equals(navKontor.remedyEnhetId())))
            .toList().size();
    }

    private List<IOrgenhet> createNewIOrgenhetList(Collection<IOrgenhet> allOrgenheter, NomNivaa nomNivaa, OrgEnhetsType orgEnhetsType) {
        return allOrgenheter.stream()
            .filter(orgenhet -> nonNull(orgenhet.getNomNivaa()))
            .filter(orgenhet -> orgenhet.getNomNivaa().equals(nomNivaa))
            .filter(orgenhet -> nonNull(orgenhet.getOrgEnhetsType()))
            .filter(orgenhet -> orgenhet.getOrgEnhetsType().equals(orgEnhetsType))
            .toList();
    }

    private boolean filterGyldigeRessurs(RessursState ressursState) {
        if (isNull(ressursState.identType())) {
            return true;
        } else {
            if (ressursState.identType().equals(IdentType.VIKAFOSSEN)) {
                return false;
            }
            return !ressursState.identType().equals(IdentType.ROBOT);
        }
    }

    private boolean erRelevantOt(OrgtilknytningState orgtilknytningState){
        var okPeriod = isPeriodActive(orgtilknytningState.gyldigFom(),orgtilknytningState.gyldigTom());
        var okNavIdent = this.ressursStateRepository.findByNavident(orgtilknytningState.navident())
            .map(this::filterGyldigeRessurs)
            .orElse(false);;
        return okPeriod && okNavIdent;
    }

    @Cacheable(cacheNames = STATISTIKK_REST_SERVICE_CACHE_NAME, cacheManager = "cacheManager")
    public OrgEnhetStatistikkResponse getStatistikkForOrgEnhetCache(String orgEnhet) {
        return hentStatistikkForOrgEnhet(orgEnhet);
    }

    @CacheEvict(value = STATISTIKK_REST_SERVICE_CACHE_NAME, allEntries = true)
    @Scheduled(fixedDelayString = "12h")
    public void emptyCache() {
        log.info("Emptying Statistikk cache");
        loadStatistikkCache();
    }

    private boolean isInitialStateReady() {
        return  nomStateRessursListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetListener.hasReadToEndOnStartup()
            && nomStateOrganiseringListener.hasReadToEndOnStartup()
            && nomStateOrgtilknytningListener.hasReadToEndOnStartup();
    }
}

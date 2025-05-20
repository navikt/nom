package no.nav.nom.apps.api.statistikk.api;


import io.getunleash.Unleash;
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
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodActive;

@Slf4j
@Service
public class StatistikkRestService {

    private final RessursStateRepository ressursStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgEnhetMapper orgEnhetMapper;
    private final Unleash unleash;

    public StatistikkRestService(RessursStateRepository ressursStateRepository,
                                 OrganiseringStateRepository organiseringStateRepository,
                                 OrgenhetService orgenhetService,
                                 RemedyEnhetMapperService remedyEnhetMapperService,
                                 OrgEnhetMapper orgEnhetMapper,
                                 Unleash unleash
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.unleash = unleash;
    }

    public GlobalStatistikkResponse hentGlobalStatistikk() {
        var alleRessurser = ressursStateRepository.findAllActiveRessurser().values().stream()
            .distinct()
            .filter(ressursState -> ressursState.startdato().isPresent() && !ressursState.startdato().get().isAfter(LocalDate.now()))
            .filter(this::filterGyldigeRessurs)
            .toList();
        var ressursStatistikk = new RessursStatistikk(
            alleRessurser.size(),
            alleRessurser.stream().filter(RessursState::isStatlig).count(),
            alleRessurser.stream().filter(RessursState::isKommunal).count(),
            alleRessurser.stream().filter(RessursState::isEkstern).count()
        );

        var alleOrgEnheter = orgenhetService.getAllEnheterMedOrganisering().stream().toList();
        var antallLovNoder = orgenhetService.getAllOrgEnheterThatCanHaveRessurser().size();

        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER);
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING);
        var antallKlageinstanser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.KLAGEINSTANS);
        var antallNavKontaktsenter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.NAV_KONTAKTSENTER);
        var antallKontrollKonrollenheter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(alleOrgEnheter, OrgEnhetsType.KONTROLL_KONTROLLENHET);
        var antallFylker = createNewIOrgenhetList(alleOrgEnheter, NomNivaa.DRIFTSENHET, OrgEnhetsType.FYLKE).size();
        int antallNavKontorer;
        if (unleash.isEnabled("nom-api.nav-kontor-alternativ-telling")) {
            antallNavKontorer = finnAlleNavKontorer(alleOrgEnheter);
        } else {
            antallNavKontorer = createNewIOrgenhetList(alleOrgEnheter, NomNivaa.ARBEIDSOMRAADE, OrgEnhetsType.NAV_KONTOR).size();
        }
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

    public OrgEnhetStatistikkResponse hentStatistikkForOrgEnhet(String orgEnhetId) {
        var optionalOrgEnhet = orgenhetService.getById(orgEnhetId);
        if (optionalOrgEnhet.isEmpty()) {
            throw new OrgEnhetNotFoundException("Fant ikke orgenhet med id " + orgEnhetId);
        }

        var underliggendeOrgEnheter = orgenhetService.getAllUnderliggendeOrgEnhetById(optionalOrgEnhet.get().getId()).stream().toList();


        var underliggendeOrgEnheterMapped = Stream.concat(Stream.of(optionalOrgEnhet.get()), underliggendeOrgEnheter.stream())
            .map(orgEnhetMapper::mapToOrgEnhet).toList();

        var alleRessursNavidenter = underliggendeOrgEnheterMapped.stream()
            .map(OrgEnhet::ressursKoblinger)
            .flatMap(Collection::stream)
            .map(OrgtilknytningState::navident)
            .distinct()
            .toList();

        var alleRessurser = ressursStateRepository.findByNavidentBulk(alleRessursNavidenter).values().stream()
            .distinct()
            .filter(ressursState -> isPeriodActive(ressursState.startdato(), ressursState.sluttdato()))
            .filter(this::filterGyldigeRessurs)
            .toList();

        var ressursStatistikk = RessursStatistikk.builder()
            .antallAktiveRessurser(alleRessurser.size())
            .antallStatligeRessurser(alleRessurser.stream().filter(RessursState::isStatlig).count())
            .antallKommunaleRessurser(alleRessurser.stream().filter(RessursState::isKommunal).count())
            .antallEksterneRessurser(alleRessurser.stream().filter(RessursState::isEkstern).count())
            .build();

        var antallLovnoder = orgenhetService.getAllOrgEnheterThatCanHaveRessurser(optionalOrgEnhet.get().getId()).size();

        if (optionalOrgEnhet.get().getNomNivaa().isPresent()) {
            if (optionalOrgEnhet.get().getNomNivaa().get().equals(NomNivaa.LINJEENHET)) {
                return hentStatistikkForLinjeenhet(ressursStatistikk, antallLovnoder, underliggendeOrgEnheter);
            } else if (optionalOrgEnhet.get().getNomNivaa().get().equals(NomNivaa.DRIFTSENHET)) {
                return hentStatistikkForDriftsenhet(ressursStatistikk, antallLovnoder, underliggendeOrgEnheter);
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

    private OrgEnhetStatistikkResponse hentStatistikkForLinjeenhet(RessursStatistikk ressursStatistikk, int antallLovnoder, Collection<IOrgenhet> underliggendeOrgEnheter) {
        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER);
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING);
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

    private OrgEnhetStatistikkResponse hentStatistikkForDriftsenhet(RessursStatistikk ressursStatistikk, int antallLovnoder, Collection<IOrgenhet> underliggendeOrgEnheter) {
        var antallNavArbeidOgYtelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_ARBEID_OG_YTELSER);
        var antallNavFamilieOgPensjonsytelser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_FAMILIE_OG_PENSJONSYTELSER);
        var antallHjelpemidlerOgTilrettelegging = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.HJELPEMIDLER_OG_TILRETTELEGGING);
        var antallKlageinstanser = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KLAGEINSTANS);
        var antallNavKontaktsenter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.NAV_KONTAKTSENTER);
        var antallKontrollKonrollenheter = getAntallOrgEnhetsTyperBasedOnOrgEnhetsType(underliggendeOrgEnheter, OrgEnhetsType.KONTROLL_KONTROLLENHET);
        var antallFylker = createNewIOrgenhetList(underliggendeOrgEnheter, NomNivaa.DRIFTSENHET, OrgEnhetsType.FYLKE).size();
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

    private int finnAlleNavKontorer(Collection<IOrgenhet> alleOrgenheter) {

        var alleNavKontorer = alleOrgenheter.stream()
            .filter(iOrgenhet -> iOrgenhet.getOrgEnhetsType().isPresent())
            .filter(iOrgenhet -> iOrgenhet.getOrgEnhetsType().get().equals(OrgEnhetsType.NAV_KONTOR))
            .toList();

        var navKontorOverordnetId = alleNavKontorer.stream()
            .map(IOrgenhet::getId)
            .map(organiseringStateRepository::findActiveById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(OrganiseringState::idOver)
            .toList();

        var antallNavKontorer = navKontorOverordnetId.stream()
            .filter(nomId -> alleOrgenheter.stream()
                .anyMatch(iOrgenhet -> iOrgenhet.getId().equals(nomId)
                    && iOrgenhet.getNomNivaa().isPresent()
                    && (!iOrgenhet.getNomNivaa().get().equals(NomNivaa.ARBEIDSOMRAADE) && iOrgenhet.getNomNivaa().isPresent())))
            .toList();

        return antallNavKontorer.size();
    }

    private List<IOrgenhet> createNewIOrgenhetList(Collection<IOrgenhet> allOrgenheter, NomNivaa nomNivaa, OrgEnhetsType orgEnhetsType) {
        return allOrgenheter.stream()
            .filter(orgenhet -> orgenhet.getNomNivaa().isPresent())
            .filter(orgenhet -> orgenhet.getNomNivaa().get().equals(nomNivaa))
            .filter(orgenhet -> orgenhet.getOrgEnhetsType().isPresent())
            .filter(orgenhet -> orgenhet.getOrgEnhetsType().get().equals(orgEnhetsType))
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
}

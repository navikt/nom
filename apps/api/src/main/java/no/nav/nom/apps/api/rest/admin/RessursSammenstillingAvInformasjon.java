package no.nav.nom.apps.api.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingStrukturerer;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class RessursSammenstillingAvInformasjon {
    public static final String RESSURS_SAMMENSTILLING = "RessursSammenstillingAvInformasjon";
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    public static String kommunaleRessurserResponseNode;
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final NomMastered nomMastered;
    private final AsyncAdminRunStatus asyncAdminRunStatus;
    private List<RessursSammenstilling> runResult;
    private Map<String, NomOrgenhet> mapAvNomOrgenhetPerOrgenhetId = new HashMap<>();

    public RessursSammenstillingAvInformasjon(
            RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService, OrgenhetService orgenhetService,
            RemedyEnhetIdResolver remedyEnhetIdResolver, OrganiseringStateRepository organiseringStateRepository, NomProxyClient nomProxyClient, AppProperties appProperties,
            @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
            @Value("${app.dvh-load.nom-kommunale-ressurser-respons-navn}") String kommunaleRessurserResponseNode,
            RessursStillingStrukturerer ressursStillingStrukturerer,
            NomMastered nomMastered, AsyncAdminRunStatus asyncAdminRunStatus, LeaderElection leaderElection
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.organiseringStateRepository = organiseringStateRepository;
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.kommunaleRessurserResponseNode = kommunaleRessurserResponseNode;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.nomMastered = nomMastered;
        this.asyncAdminRunStatus = asyncAdminRunStatus;
    }

    @Async
    public void runSammenstilling(String sektor, String navidenter, boolean bareAktive) {
        var sammenstilling = getNomRessursInfo(sektor, navidenter, bareAktive);

        var navidenterINom = sammenstilling.stream().map(RessursSammenstilling::getNavident).toList();
        log.info("Laget sammenstilling av ressursinformasjon fra NOM for {} ressurser", sammenstilling.size());

        var dvhRessurserData = getDvhRessursInfo(navidenterINom, sektor != null && sektor.equals(Sektor.NAV_KOMMUNAL.name()));
        sammenstilling.forEach(r -> r.setDvhRessursData(dvhRessurserData.get(r.getNavident())));

        var dvhRessurserStillinger = getDvhRessursStillingInfo(navidenterINom);
        sammenstilling.forEach(r -> r.setDvhRessursStillingData(dvhRessurserStillinger.get(r.getNavident())));

        var orgtilknytningerINom = getOrgiltknytningerINom(navidenterINom);
        sammenstilling.forEach(r -> r.setOrgtilknytningerINom(orgtilknytningerINom.get(r.getNavident())));

        sammenstilling.forEach(r -> r.setErMastretINom(nomMastered.isMastered(r.getNavident())));

        runResult = sammenstilling;
        asyncAdminRunStatus.runEnded(RESSURS_SAMMENSTILLING);
    }

    public List<RessursSammenstilling> runResult() { return runResult; }

    private List<RessursSammenstilling> getNomRessursInfo(String sektor, String navidenter, boolean bareAktive) {
        var ressursStream = bareAktive ?
            ressursStateRepository.getAll().stream()
                .filter(r -> (r.startdato().isEmpty() || r.startdato().get().isBefore(LocalDate.now()))
                    && (r.sluttdato().isEmpty() || r.sluttdato().get().isAfter(LocalDate.now()))
                )
            : ressursStateRepository.getAll().stream();

        if (sektor != null && !sektor.trim().isEmpty()) {
            ressursStream = ressursStream.filter(r -> r.sektor().equals(Sektor.valueOf(sektor.trim())));
        }

        var navidenterListe = navidenter == null || navidenter.isBlank() ? List.of() : Arrays.stream(navidenter.split(",")).map(String::trim).toList();
        if (!navidenterListe.isEmpty()) {
            ressursStream = ressursStream.filter(r -> navidenterListe.contains(r.navident()));
        }

        var sammenstilling = ressursStream.map(RessursSammenstilling::new).toList();
        return sammenstilling;
    }

    private Map<String, DvhRessursData> getDvhRessursInfo(List<String> navidenterINom, boolean isKommunale) {
        var dvhRessurserData = getDvhRessurser(isKommunale).stream()
            .filter(dvhRessurs -> navidenterINom.contains(dvhRessurs.getNavIdent()))
            .collect(Collectors.toMap(DvhRessurs::getNavIdent, r -> new DvhRessursData(r, getOrgenhetPaaRessurs(r))));

        String url = isKommunale ? appProperties.clients().nomProxy().kommunaleRessurserPath() : appProperties.clients().nomProxy().ansatteBucketsPath();
        log.info("Lastet {} ressurser fra DVH [{}]", dvhRessurserData.size(), url);
        return dvhRessurserData;
    }

    private Map<String, DvhRessursStillingData> getDvhRessursStillingInfo(List<String> navidenterINom) {
        var dvhRessurseStillinger = getDvhRessurserStillinger();

        Map<String, List<StillingPeriode>> stillingPerioderPerNavident = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(dvhRessurseStillinger);
        int stillingePerioder = stillingPerioderPerNavident.values().stream().mapToInt(List::size).sum();

        Map<String, DvhRessursStillingData> dvhRessurserData = dvhRessurseStillinger.stream()
            .filter(dvhRessursStilling -> dvhRessursStilling.getNavident().isPresent() && navidenterINom.contains(dvhRessursStilling.getNavident().get()))
            .collect(Collectors.toMap( r -> r.getNavident().get(),
                r -> new DvhRessursStillingData(r, getOrgenhetPaaRessurs(r)),
                (r1, r2) -> r2));

        dvhRessurserData.forEach( (navident, dvhRessursStillingData) -> {
            var sp = stillingPerioderPerNavident.get(navident);
            dvhRessursStillingData.stillingPerioder = ( sp == null ?  Collections.emptyList() :
                    sp.stream()
                        .map(s -> new DvhRessursStillingPeriode(s, getNomOrgenhet(s.orgenhet())))
                        .toList()
                );
        });

        log.info("Lastet {} DVH stillingperioder for  {} ressurser fra DVH [{}]", stillingePerioder, stillingPerioderPerNavident.size(), appProperties.clients().nomProxy().ressursStillingerPath());
        return dvhRessurserData;
    }


    private Map<String, List<NomOrgtilknytning>> getOrgiltknytningerINom(List<String> navidenterINom) {
        Map<String, List<NomOrgtilknytning>> orgtilknytningerINom = new HashMap<>();
        orgtilknytningService.getAll().stream()
            .filter(orgtilknytning -> navidenterINom.contains(orgtilknytning.navident()))
            .forEach(orgtilknytning -> {
                orgtilknytningerINom.computeIfAbsent(orgtilknytning.navident(), k -> new java.util.ArrayList<>());
                orgtilknytningerINom.get(orgtilknytning.navident()).add(
                    new NomOrgtilknytning(
                        getNomOrgenhetById(orgtilknytning.orgEnhetId()),
                        orgtilknytning.gyldigFom(),
                        orgtilknytning.gyldigTom().orElse(null)
                    )
                );
            });
        return orgtilknytningerINom;
    }

    private List<DvhRessursStilling> getDvhRessurserStillinger() {
        val jsonRessursStillinger = dvhExchange(appProperties.clients().nomProxy().ressursStillingerPath());

        var dvhRessursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
            .map(DvhRessursStilling::new)
            .toList();
        return dvhRessursStillinger;
    }

    private List<DvhRessurs> getDvhRessurser(boolean isKommunale) {
        String url = isKommunale ? appProperties.clients().nomProxy().kommunaleRessurserPath() : appProperties.clients().nomProxy().ansatteBucketsPath();

        var jsonRessurser = dvhExchange(url);
        var dvhRessurser = StreamSupport.stream(jsonRessurser.get(isKommunale ? kommunaleRessurserResponseNode : "NOM_ANSATTE_BUCKETS_v2Response").spliterator(), true)
            .map( r -> new DvhRessurs(r, isKommunale))
            .distinct()
            .toList();
        return dvhRessurser;
    }

    private JsonNode dvhExchange(String urlPath) {
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private NomOrgenhet getOrgenhetPaaRessurs(DvhRessurs dvhRessurs) {
        var orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
            dvhRessurs.getOrgenhetPaaRessurs(),
            dvhRessurs.getOrgenhetPaaRessursNiva(),
            dvhRessurs.getOrgenhetPaaRessursBeskrivelse ()
        );
        return orgenhet.map(this::getNomOrgenhet).orElse(null);
    }

    private NomOrgenhet getOrgenhetPaaRessurs(DvhRessursStilling dvhRessursStilling) {
        // Uten navn - slå opp på kun AgressoId og Nivå
        var orgenhet = dvhRessursStilling.getStillingOrgenhetNavn() == null
            ? orgenhetService.getEnhetByAgressoIdAndOrgniv(
                dvhRessursStilling.getRessursAgressoId().orElse(null),
                dvhRessursStilling.getRessursOrgenhetNiv().orElse(null))
            : orgenhetService.getEnhetByAgressoIdNivaAndName(
                dvhRessursStilling.getRessursAgressoId().orElse(null),
                dvhRessursStilling.getRessursOrgenhetNiv().orElse(null),
                dvhRessursStilling.getStillingOrgenhetNavn());
        return orgenhet.map(this::getNomOrgenhet).orElse(null);
    }

    private NomOrgenhet getNomOrgenhetById(String id) {
        var orgenhet = orgenhetService.getById(id);
        return orgenhet.map(this::getNomOrgenhet).orElse(null);
    }

    private NomOrgenhet getNomOrgenhet(IOrgenhet orgenhet) {
        if (orgenhet == null) {
            return null;
        }
        var nomOrgenhet = mapAvNomOrgenhetPerOrgenhetId.get(orgenhet.getId());
        if (nomOrgenhet == null) {
            nomOrgenhet = new NomOrgenhet(orgenhet, getOverodnetOrgenhet(orgenhet), remedyEnhetIdResolver.remedyEnhetId(orgenhet));
            if (nomOrgenhet != null) {
                mapAvNomOrgenhetPerOrgenhetId.put(orgenhet.getId(), nomOrgenhet);
            }
        }
        return nomOrgenhet;
    }

    private IOrgenhet getOverodnetOrgenhet(IOrgenhet orgenhet) {
        var overodnet =  organiseringStateRepository.getOrganiseringerContainingId(orgenhet.getId()).stream()
            .filter( organisering -> organisering.id().equals(orgenhet.getId()))  // Kun organisering hvor denne enhet er underenhet
            .sorted(Comparator.comparing(OrganiseringState::gyldigFom).reversed())  // Sorter på gyldigFom, nyere først
            .findFirst()// Plukk den nyeste
            .flatMap(o -> orgenhetService.getById(o.idOver())).orElse(null);
        return overodnet;
    }


    @Data
    @AllArgsConstructor
    public static class RessursSammenstilling {
        String navident;
        Visningsnavn visningsnavn;
        Sektor nomSektor;
        LocalDate nomStartdato;
        LocalDate nomSluttdato;
        boolean erMastretINom;
        List<NomOrgtilknytning> orgtilknytningerINom;
        DvhRessursData dvhRessursData;
        DvhRessursStillingData dvhRessursStillingData;

        RessursSammenstilling(RessursState ressursState) {
            this(
                ressursState.navident(),
                ressursState.visningsnavn(),
                ressursState.sektor(),
                ressursState.startdato().orElse(null),
                ressursState.sluttdato().orElse(null),
                false,
                null,
                null,
                null
            );
        }
    }

    record DvhRessursData(
        boolean remedyAktiv,
        String remedyAnsattType,
        String u4AnsattRessursKode,
        LocalDate remedyStartDato,
        LocalDate remedySluttDato,
        LocalDate u4StartDato,
        LocalDate u4Sluttdato,
        String dvhOrgenhetPaaRessurs,
        String dvhOrgenhetPaaRessursBeskrivelse,
        String dvhOrgenhetPaaRessursNiva,
        NomOrgenhet nomOrgenhetPaaRessurs,
        boolean remedyPermisjon,
        String remedyIdentType
    ) {
        DvhRessursData(DvhRessurs dvhRessurs, NomOrgenhet nomOrgenhetPaaRessurs) {
            this(
                dvhRessurs.getRemedyAktiv(),
                dvhRessurs.getRemedyAnsattType(),
                dvhRessurs.getAnsattRessursKode(),
                dvhRessurs.getRemedyStartDato(),
                dvhRessurs.getRemedySluttdato(),
                dvhRessurs.getAgressoStartDato(),
                dvhRessurs.getAgressoSluttdato(),
                dvhRessurs.getOrgenhetPaaRessurs(),
                dvhRessurs.getOrgenhetPaaRessursBeskrivelse(),
                dvhRessurs.getOrgenhetPaaRessursNiva(),
                nomOrgenhetPaaRessurs,
                dvhRessurs.isRemedyPermisjon(),
                dvhRessurs.getIdentType()
            );
        }
    }

    @Data
    @AllArgsConstructor
    static class DvhRessursStillingData{
        LocalDate u4StartDato;
        LocalDate u4Sluttdato;
        String orgenhetPaaRessursAgressoId;
        String orgenhetPaaRessursAgressoOrgNiv;
        String remedyEnhetIdPaaRessurs;
        NomOrgenhet nomOrgenhetPaaRessurs;
        List<DvhRessursStillingPeriode> stillingPerioder;
        DvhRessursStillingData(DvhRessursStilling dvhRessursStilling, NomOrgenhet nomOrgenhetPaaRessurs) {
            this(
                dvhRessursStilling.getRessursAgressoDatoFom(),
                dvhRessursStilling.getRessursAgressoDatoTom(),
                dvhRessursStilling.getRessursAgressoId().orElse(null),
                dvhRessursStilling.getRessursOrgenhetNiv().orElse(null),
                dvhRessursStilling.getRessursRemedyEnhetId().orElse(null),
                nomOrgenhetPaaRessurs,
                null
            );
        }
    }

    record DvhRessursStillingPeriode(
        LocalDate stillingGyldigFom,
        LocalDate stillingGyldigTom,
        String stillingAgressoId,
        String stillingAgressoOrgenhetNavn,
        NomOrgenhet stillingOrgenhet
    ) {
        DvhRessursStillingPeriode(StillingPeriode stillingPeriode, NomOrgenhet nomOrgenhet) {
            this(
                stillingPeriode.gyldigFom(),
                stillingPeriode.gyldigTom(),
                stillingPeriode.stilling().agressoId().orElse(null),
                stillingPeriode.stilling().enhetNavn(),
                nomOrgenhet
            );
        }
    }

    record NomOrgenhet(
        String fid,
        String id,
        String navn,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        String nomRemedyEnhetId,
        String OverodnetOrgenhetId,
        String OverodnetOrgenhetNavn
    ){
        NomOrgenhet(IOrgenhet orgenhet, IOrgenhet overordnetOrgenhet, String nomRemedyEnhetId) {
            this(
                orgenhet.getFid(),
                orgenhet.getId(),
                orgenhet.getNavn(),
                orgenhet.getGyldigFom(),
                orgenhet.getGyldigTom().orElse(null),
                nomRemedyEnhetId,
                overordnetOrgenhet != null ? overordnetOrgenhet.getId() : null,
                overordnetOrgenhet != null ? overordnetOrgenhet.getNavn() : null
            );
        }
    }

    record NomOrgtilknytning(
        NomOrgenhet orgenhet,
        LocalDate GyldigFom,
        LocalDate GyldigTom
    ){}

}


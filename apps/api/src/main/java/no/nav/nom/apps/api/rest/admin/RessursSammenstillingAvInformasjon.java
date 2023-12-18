package no.nav.nom.apps.api.rest.admin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.RemedyEnhetIdResolver;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingStrukturerer;
import no.nav.nom.apps.api.orgtilknytning.dvh.StillingPeriode;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.masterdata.NomMasteredRessursListener;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Tag(name = "Admin for datagrunnlag og sammenstilling")
@RestController
@RequestMapping("/api/admin/RessursSammenstillingAvInformasjon")
public class RessursSammenstillingAvInformasjon {
    public static final String DVH_RESSURSER_URL = "/proxy/dvh/json/Hr/NOM3/NOM_ANSATTE_BUCKETS_v2";
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final RemedyEnhetIdResolver remedyEnhetIdResolver;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final NomMasteredRessursListener nomMasteredRessursListener;

    public RessursSammenstillingAvInformasjon(
            RessursStateRepository ressursStateRepository, OrgtilknytningService orgtilknytningService, OrgenhetService orgenhetService,
            RemedyEnhetIdResolver remedyEnhetIdResolver, OrganiseringStateRepository organiseringStateRepository, NomProxyClient nomProxyClient, AppProperties appProperties,
            @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode, RessursStillingStrukturerer ressursStillingStrukturerer,
            NomMasteredRessursListener nomMasteredRessursListener
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.remedyEnhetIdResolver = remedyEnhetIdResolver;
        this.organiseringStateRepository = organiseringStateRepository;
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.nomMasteredRessursListener = nomMasteredRessursListener;
    }

    @Operation(
        description = "Sammenstiller informasjon fra NOM og DVH (U4/Remedy)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping
    public List<RessursSammenstilling> lagSammenstilling(
        @Parameter(description = "Begrense informasjonen til Sektor (NAV_STATLIG, NAV_KOMMUNAL, EKSTERN). For alle, la stå tom") @RequestParam(required = false) String sektor,
        @Parameter(description = "Begrense til angitte navidenter - kommaseparert. Las stå tom for alle") @RequestParam(required = false) String navidenter,
        @Parameter(description = "Kun aktive? default false") @RequestParam(defaultValue = "true") boolean bareAktive,
        @NonNull JwtAuthenticationToken token
    ) {
        var sammenstilling = getNomRessursInfo(sektor, navidenter, bareAktive);

        var navidenterINom = sammenstilling.stream().map(RessursSammenstilling::getNavident).toList();
        log.info("Laget sammenstilling av ressursinformasjon fra NOM for {} ressurser", sammenstilling.size());

        var dvhRessurserData = getDvhRessursInfo(navidenterINom);
        sammenstilling.forEach(r -> r.setDvhRessursData(dvhRessurserData.get(r.getNavident())));

        var dvhRessurserStillinger = getDvhRessursStillingInfo(navidenterINom);
        sammenstilling.forEach(r -> r.setDvhRessursStillingData(dvhRessurserStillinger.get(r.getNavident())));

        return sammenstilling;
    }

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

    private Map<String, DvhRessursData> getDvhRessursInfo(List<String> navidenterINom) {
        var dvhRessurserData = getDvhRessurser().stream()
            .filter(dvhRessurs -> navidenterINom.contains(dvhRessurs.getNavIdent()))
            .collect(Collectors.toMap(DvhRessurs::getNavIdent, DvhRessursData::new));

        log.info("Lastet {} ressurser fra DVH [{}]", dvhRessurserData.size(), DVH_RESSURSER_URL);
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
                    sp.stream().map(s -> {
                        IOrgenhet overodnet = organiseringStateRepository.findByFid(s.orgenhet().getFid()).flatMap(o -> orgenhetService.getById(o.id())).orElse(null);
                        String remedyEnhetId = remedyEnhetIdResolver.remedyEnhetId(s.orgenhet());
                        return new DvhRessursStillingPeriode(s, s.orgenhet(), overodnet, remedyEnhetId);
                    }).toList()
                );
        });

        log.info("Lastet {} DVH stillingperioder for  {} ressurser fra DVH [{}]", stillingePerioder, stillingPerioderPerNavident.size(), appProperties.clients().nomProxy().ressursStillingerPath());
        return dvhRessurserData;
    }

    private List<DvhRessursStilling> getDvhRessurserStillinger() {
        val jsonRessursStillinger = dvhExchange(appProperties.clients().nomProxy().ressursStillingerPath());

        var dvhRessursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
            .map(DvhRessursStilling::new)
            .toList();
        return dvhRessursStillinger;
    }

    private List<DvhRessurs> getDvhRessurser() {
        var jsonRessurser = dvhExchange(DVH_RESSURSER_URL);
        var dvhRessurser = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v2Response").spliterator(), true)
            .map(DvhRessurs::new)
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

    private NomOrgenhet getOrgenhetPaaRessurs(DvhRessursStilling dvhRessursStilling) {
        var orgenhet = orgenhetService.getEnhetByAgressoIdNivaAndName(
            dvhRessursStilling.getRessursAgressoId().orElse(null),
            dvhRessursStilling.getRessursOrgenhetNiv().orElse(null),
            dvhRessursStilling.getStillingOrgenhetNavn());
        if (orgenhet.isEmpty()) return null;

        var overodnet =  organiseringStateRepository.getOrganiseringerContainingId(orgenhet.get().getId()).stream()
            .filter( organisering -> organisering.id().equals(orgenhet.get().getId()))  // Kun organisering hvor denne enhet er underenhet
            .sorted( (o1, o2) -> o1.gyldigFom().compareTo(o2.gyldigFom()))  // Sorter på gyldigFom, nyere først
            .findFirst()// Plukk den nyeste
            .flatMap(o -> orgenhetService.getById(o.id())).orElse(null);
        return new NomOrgenhet(orgenhet.get(), overodnet, remedyEnhetIdResolver.remedyEnhetId(orgenhet.get()));
    }


    @Data
    @AllArgsConstructor
    static class RessursSammenstilling {
        String navident;
        Visningsnavn visningsnavn;
        Sektor nomSektor;
        LocalDate nomStartdato;
        LocalDate nomSluttdato;
        DvhRessursData dvhRessursData;
        DvhRessursStillingData dvhRessursStillingData;

        RessursSammenstilling(RessursState ressursState) {
            this(
                ressursState.navident(),
                ressursState.visningsnavn(),
                ressursState.sektor(),
                ressursState.startdato().orElse(null),
                ressursState.sluttdato().orElse(null),
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
        boolean remedyPermisjon,
        String remedyIdentType
    ) {
        DvhRessursData(DvhRessurs dvhRessurs) {
            this(
                dvhRessurs.getRemedyAktiv(),
                dvhRessurs.getRemedyAnsattType(),
                dvhRessurs.getAnsattRessursKode(),
                dvhRessurs.getRemedyStartDato(),
                dvhRessurs.getRemedySluttdato(),
                dvhRessurs.getAgressoStartDato(),
                dvhRessurs.getAgressoSluttdato(),
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
        DvhRessursStillingPeriode(StillingPeriode stillingPeriode, IOrgenhet nomEnhet, IOrgenhet nomOverordnetEnhet, String nomRemedyEnhetId) {
            this(
                stillingPeriode.gyldigFom(),
                stillingPeriode.gyldigTom(),
                stillingPeriode.stilling().agressoId().orElse(null),
                stillingPeriode.stilling().enhetNavn(),
                new NomOrgenhet(nomEnhet, nomOverordnetEnhet, nomRemedyEnhetId)
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
}


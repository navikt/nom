package no.nav.nom.apps.api.teamcat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.app.RessursLedereResolver;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Teamkatalog")
@RestController
@RequestMapping("/api/v1/teamcat")
@PreAuthorize("hasAuthority(\"TEAMCAT\")")
public class TeamcatController {


    private final AdClient adclient;
    private final DvhRepository dvhRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetOverordnetResolver overordnetResolver;
    private final RessursLedereResolver ressursLedereResolver;
    private final String IT_AVDELING_ID;
    private final OrgEnhetMapper orgEnhetMapper;

    public TeamcatController(
        RessursStateRepository ressursStateRepository,
        AdClient adclient,
        DvhRepository dvhRepository,
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        Environment env,
        OrgEnhetOverordnetResolver overordnetResolver,
        RessursLedereResolver ressursLedereResolver,
        OrgEnhetMapper orgEnhetMapper
    ) {
        this.adclient = adclient;
        this.dvhRepository = dvhRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.ressursStateRepository = ressursStateRepository;
        this.overordnetResolver = overordnetResolver;
        this.ressursLedereResolver = ressursLedereResolver;
        this.orgEnhetMapper = orgEnhetMapper;
        var isDev = "dev-gcp".equals(env.getProperty("NAIS_CLUSTER_NAME"));
        this.IT_AVDELING_ID = isDev ? "hu357k" : "ve673e";
    }


    private String determineAvdelingNavnByEnhet(IOrgenhet inputEnhet, PerRequestCache perRequestCache) {
        var maybeCachedResult = perRequestCache.getNameIfCached(inputEnhet.getId());
        if (maybeCachedResult != null) {
            return maybeCachedResult;
        }

        var hierarchy = new LinkedList<IOrgenhet>();
        hierarchy.addFirst(inputEnhet);

        var enhet = Optional.of(inputEnhet);

        // build hierarchy
        while (enhet.isPresent()) {
            var overenhetidByResolver = overordnetResolver.findOverordnetOrgEnhet(enhet.get().getFid()).map(OrgEnhetState::fid);
            var overenhetid = enhet.flatMap(IOrgenhet::getOverordnetOrgEnhetFid).or(() -> overenhetidByResolver);

            if (overenhetid.isEmpty()) {
                enhet = Optional.empty();
            } else {
                enhet = orgenhetService.getByFid(overenhetid.get());
                enhet.ifPresent(hierarchy::addFirst);
            }
        }

        String out;
        switch (hierarchy.size()) {
            case 1 -> {
                out = hierarchy.get(0).getNavn();
            }
            case 2 -> {
                out = hierarchy.get(1).getNavn();
            }
            default -> {
                var includesItDept = hierarchy.stream().filter(it -> it.getFid().equals(IT_AVDELING_ID)).findFirst();
                if (includesItDept.isPresent()) {
                    var itDeptIdx = hierarchy.indexOf(includesItDept.get());
                    var subItIdx = itDeptIdx + 1;
                    if (subItIdx < hierarchy.size()) {
                        var subItDept = hierarchy.get(subItIdx);
                        return subItDept.getNavn();
                    }
                }
                out = hierarchy.get(2).getNavn();

            }
        }
        perRequestCache.putEnhetToName(inputEnhet.getId(), out);
        return out;
    }

    @Operation(
        description = "Returnerer en teamkatalog ressurs",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("ressurs/{navident}")
    public ResponseEntity<TeamcatRessursDTO> ressurs(@PathVariable("navident") String navident) {
        navident = navident.toUpperCase();

        var adResource = adclient.fetchInAdRessursInBatches(List.of(navident));

        var maybeRessurs = ressursStateRepository.findByNavident(navident);

        if (maybeRessurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var ressurs = maybeRessurs.get();

        var out = getTeamcatRessursDTO(navident, ressurs, adResource);

        return ResponseEntity.ok(out);
    }

    private TeamcatRessursDTO getTeamcatRessursDTO(String navident, RessursState ressurs, Map<String, AdRessurs> adResource, PerRequestCache perRequestCache) {

        var oeId = orgtilknytningService.getByNavIdent(navident)
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .findFirst()
            .map(OrgtilknytningState::orgEnhetId)
            .orElse(null);
        var lederNavident = ressursLedereResolver.ledere(navident)
            .stream()
            .filter(RessursLeder::erDagligOppfolging)
            .findFirst()
            .map(RessursLeder::leder)
            .map(RessursState::navident)
            .orElse(null);


        return new TeamcatRessursDTO(
            ressurs.startdato().orElse(null),
            ressurs.sluttdato().orElse(null),
            adResource.get(navident) != null ? adResource.get(navident).getEmail() : null,
            navident,
            TeamcatRessursDTO.ResourceType.from(ressurs.sektor(), dvhRepository.getIsAnnenStat(navident)),
            ressurs.visningsnavn().getFormattedVisningsnavn(),
            oeId,
            lederNavident
        );
    }

    private TeamcatRessursDTO getTeamcatRessursDTO(String navident, RessursState ressurs, Map<String, AdRessurs> adResource) {
        return getTeamcatRessursDTO(navident, ressurs, adResource, new PerRequestCache());
    }


    @Operation(
        description = "Hent teamkatalog ressurser i batch",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("ressurser")
    public ResponseEntity<Map<String, TeamcatRessursDTO>> ressurser(@RequestBody List<String> navidentListInput) {
        var maybeBadNavident = navidentListInput.stream().filter(s -> !s.matches("[a-zA-Z]\\d{6}")).findFirst();
        if (maybeBadNavident.isPresent()) {
            throw new IllegalArgumentException("Bad navident: " + maybeBadNavident.get());
        }

        navidentListInput = navidentListInput.stream().map(String::toUpperCase).toList();

        var adResource = adclient.fetchInAdRessursInBatches(navidentListInput);

        var maybeRessursList = ressursStateRepository.findByNavidentBulk(navidentListInput);
        var out = new HashMap<String, TeamcatRessursDTO>(maybeRessursList.size());

        var perRequestCache = new PerRequestCache();
        for (var x : maybeRessursList.entrySet()) {
            var ressurs = x.getValue();
            var navident = x.getKey();
            var tmp = getTeamcatRessursDTO(navident, ressurs, adResource, perRequestCache);
            out.put(navident, tmp);
        }


        return ResponseEntity.ok(out);
    }

    @Operation(
        description = "Hent alle teamkatalog ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/alleRessurser")
    public ResponseEntity<Map<String, TeamcatRessursDTO>> alleRessurser() {
        var alleressurser = ressursStateRepository.getAll();
        var adResources = adclient.fetchInAdRessursInBatches(alleressurser.stream()
            // only active resources are available in ad
            .filter(it -> it.sluttdato().map(sluttdato -> sluttdato.isAfter(LocalDate.now())).orElse(true))
            .map(RessursState::navident).toList());

        var perRequestCache = new PerRequestCache();
        var out = new HashMap<String, TeamcatRessursDTO>(alleressurser.size());
        for (var r : alleressurser) {
            var tmp = getTeamcatRessursDTO(r.navident(), r, adResources, perRequestCache);
            out.put(r.navident(), tmp);
        }
        return ResponseEntity.ok(out);
    }

    @Operation(
        description = "Hent alle teamkatalog enheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/alleEnheter")
    public ResponseEntity<Map<String, TeamcatOrgenhetDTO>> alleEnheter() {
        var alleEnheter = this.orgenhetService.getAllEnheter();

        var perRequestCache = new PerRequestCache();
        var out = new HashMap<String, TeamcatOrgenhetDTO>(alleEnheter.size());
        for (var r : alleEnheter) {
            var mapped = orgEnhetMapper.mapToOrgEnhet(r);
            var lederNavident = mapped.ledere().stream().findFirst().map(RessursState::navident).orElse(null);
            var enhetOverId = mapped.overordnetEnhetId();
            var tmp = new TeamcatOrgenhetDTO(r.getId(), r.getNavn(), enhetOverId, determineAvdelingNavnByEnhet(r, perRequestCache), lederNavident);
            out.put(r.getId(), tmp);
        }
        return ResponseEntity.ok(out);
    }

    @Operation(
        description = "Hent teamkatalog enhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("enhet/{id}")
    public ResponseEntity<TeamcatOrgenhetDTO> enhet(@PathVariable("id") String id) {
        var enhetMaybe = this.orgenhetService.getById(id);

        if (enhetMaybe.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var enhet = enhetMaybe.get();
        var mapped = orgEnhetMapper.mapToOrgEnhet(enhet);
        return ResponseEntity.ok(new TeamcatOrgenhetDTO(
            enhet.getId(),
            enhet.getNavn(),
            enhet.getOverordnetOrgEnhetFid().orElse(null),
            determineAvdelingNavnByEnhet(enhet, new PerRequestCache()),
            mapped.ledere().stream().findFirst().map(RessursState::navident).orElse(null)
        ));

    }

    private static class PerRequestCache {

        private final HashMap<Object, String> enhetToName = new HashMap<>();

        public void putEnhetToName(String orgenhetId, String out) {
            this.enhetToName.put(orgenhetId, out);
        }

        public String getNameIfCached(String orgenhetId) {
            return this.enhetToName.get(orgenhetId);
        }
    }


}

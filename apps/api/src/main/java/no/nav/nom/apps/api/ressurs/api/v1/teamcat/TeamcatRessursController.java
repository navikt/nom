package no.nav.nom.apps.api.ressurs.api.v1.teamcat;

import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Temacat")
@RestController
@RequestMapping("/api/v1/teamcat")
public class TeamcatRessursController {


    private final AdClient adclient;
    private final DvhRepository dvhRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final RessursStateRepository ressursStateRepository;
    private final String IT_AVDELING_ID;

    public TeamcatRessursController(
        RessursStateRepository ressursStateRepository,
        AdClient adclient,
        DvhRepository dvhRepository,
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        Environment env
    ) {
        this.adclient = adclient;
        this.dvhRepository = dvhRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.ressursStateRepository = ressursStateRepository;
        var isDev = "dev-gcp".equals(env.getProperty("NAIS_CLUSTER_NAME"));
        this.IT_AVDELING_ID = isDev ? "hu357k" : "ve673e";
    }

    private String determineAvdelingNavn(String navident) {

        var orgtilknytningStates = orgtilknytningService.getByNavIdent(navident);
        if (orgtilknytningStates.isEmpty()) {
            return null;
        }

        var dagligOppfoelgingOrgtilknytning = orgtilknytningStates.stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .findFirst();

        var orgTilknytning = dagligOppfoelgingOrgtilknytning.orElseGet(() -> orgtilknytningStates.stream().findFirst().get());
        var enhet = orgenhetService.getByFid(orgTilknytning.fid());

        if (enhet.isEmpty()) {
            return null;
        }

        var hierarchy = new LinkedList<IOrgenhet>();
        hierarchy.addFirst(enhet.get());

        // build hierarchy
        while (enhet.isPresent()) {
            var overenhetid = enhet.flatMap(IOrgenhet::getOverordnetOrgEnhetFid);
            if (overenhetid.isEmpty()) {
                enhet = Optional.empty();
            } else {
                enhet = orgenhetService.getByFid(overenhetid.get());
                enhet.ifPresent(hierarchy::addFirst);
            }
        }

        switch (hierarchy.size()) {
            case 1 -> {
                return hierarchy.get(0).getNavn();
            }
            case 2 -> {
                return hierarchy.get(1).getNavn();
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
                return hierarchy.get(2).getNavn();

            }
        }
    }

    @GetMapping("ressurs/{navident}")
    public ResponseEntity<TeamcatRessursDTO> ressurs(@PathVariable("navident") String navident) {

        var adResource = adclient.fetchInAdRessursInBatches(List.of(navident));

        var maybeRessurs = ressursStateRepository.findByNavident(navident);

        if (maybeRessurs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var ressurs = maybeRessurs.get();

        var out = new TeamcatRessursDTO(
            ressurs.startdato().orElse(null),
            ressurs.sluttdato().orElse(null),
            adResource != null ? adResource.get(navident).getEmail() : null,
            navident,
            TeamcatRessursDTO.ResourceType.from(ressurs.sektor(), dvhRepository.getIsAnnenStat(navident)),
            ressurs.visningsnavn().getFormattedVisningsnavn(),
            determineAvdelingNavn(navident)
        );

        return ResponseEntity.ok(out);
    }

    @PostMapping("ressurser")
    public ResponseEntity<Map<String, TeamcatRessursDTO>> ressurser(@RequestBody List<String> navidentListInput) {
        var maybeBadNavident = navidentListInput.stream().filter(s -> !s.matches("[a-zA-Z]\\d{6}")).findFirst();
        if (maybeBadNavident.isPresent()) {
            throw new IllegalArgumentException("Bad navident: " + maybeBadNavident.get());
        }

        var adResource = adclient.fetchInAdRessursInBatches(navidentListInput);

        var maybeRessursList = ressursStateRepository.findByNavidentBulk(navidentListInput);
        var out = new HashMap<String, TeamcatRessursDTO>(maybeRessursList.size());

        for (var x : maybeRessursList.entrySet()) {
            var ressurs = x.getValue();
            var navident = x.getKey();
            var tmp = new TeamcatRessursDTO(
                ressurs.startdato().orElse(null),
                ressurs.sluttdato().orElse(null),
                adResource != null ? adResource.get(navident).getEmail() : null,
                navident,
                TeamcatRessursDTO.ResourceType.from(ressurs.sektor(), dvhRepository.getIsAnnenStat(navident)),
                ressurs.visningsnavn().getFormattedVisningsnavn(),
                determineAvdelingNavn(navident)
            );
            out.put(navident, tmp);
        }


        return ResponseEntity.ok(out);
    }


}

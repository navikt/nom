package no.nav.nom.apps.api.rest.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Admin for korrigere duplikate enheter")
@RestController
@RequestMapping("/api/admin/korrigerreduplikatenheter")
public class KorrigerDuplikatEnheter {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetCommandService orgEnhetCommandService;

    public KorrigerDuplikatEnheter(OrgenhetService orgenhetService, OrgEnhetCommandService orgEnhetCommandService) {
        this.orgenhetService = orgenhetService;
        this.orgEnhetCommandService = orgEnhetCommandService;
    }

    @Operation(
        description = "Lag avvikle eventer for duplikater. Kjør med False for kun dry-run",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    public String korrigere(@RequestParam(defaultValue = "false") boolean performUpdate) {
        List<IOrgenhet> alleEnheter = (List<IOrgenhet>) orgenhetService.getAllEnheter();
        List<IOrgenhet> aktiveEnheter = aktiveEnheter(alleEnheter);
        Map<String, List<IOrgenhet>> aktiveEnheterDuplikatAvSammeUniqueAgressoId = duplikatPaaSammeUniqueAgresssoId(aktiveEnheter);
        ObjectMapper mapper = new ObjectMapper();
        StringBuffer str = new StringBuffer();
        aktiveEnheterDuplikatAvSammeUniqueAgressoId.forEach( (faktiskId, duplikater) -> duplikater.forEach(duplikat -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("Duplikate of", faktiskId);
            node.put("For", duplikat.getAgressoId() + "-" + duplikat.getNavn() + "-" + duplikat.getOrgNiv());
            node.put("With period", duplikat.getGyldigFom().toString() + " - " + (duplikat.getGyldigTom().isPresent() ? duplikat.getGyldigTom().get() :  " "));
            AvvikleOrgEnhet avvikleOrgEnhet =
                new AvvikleOrgEnhet(
                    duplikat.getGyldigFom(),
                    new InputMetadata(
                        "Swagger",
                        "Korriger duplikate aktive org enheter",
                        node,
                        new Origination("nom-api", "nom-api")
                    )
                );
            String eventStr =  "AvikleEvent for fid=%s, id=%s, date=%s og inputmeta=%s".formatted(duplikat.getFid(), duplikat.getId(), avvikleOrgEnhet.gyldigTom(), avvikleOrgEnhet.inputMetadata());
            str.append(eventStr).append("\n");
            log.info(eventStr);
            if (performUpdate) orgEnhetCommandService.avikleOrgEnhet(duplikat.getFid(), avvikleOrgEnhet);
        }));
        return str.toString();
    }

    private List<IOrgenhet> aktiveEnheter(List<IOrgenhet> enheter) {
        // FOM er før eller lik dagens dat, samt at TOM er null eller fremtidig
        return enheter.stream().filter( oe ->
            oe.getGyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (oe.getGyldigTom().isEmpty() || oe.getGyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private Map<String, List<IOrgenhet>> duplikatPaaSammeUniqueAgresssoId(List<IOrgenhet> aktiveEnheter) {
        var duplicateListMap = new HashMap<String, List<IOrgenhet>>();
        aktiveEnheter.forEach( e -> {
                IOrgenhet enhetPaaUniqueAgressoId = orgenhetService.getEnhetByAgressoIdNivaAndName(e.getAgressoId(), e.getOrgNiv(), e.getNavn()).get();
                if (!enhetPaaUniqueAgressoId.getId().equals(e.getId())) {
                    List<IOrgenhet> duplicates = duplicateListMap.get(enhetPaaUniqueAgressoId.getId());
                    if (duplicates == null) {
                        duplicates = new ArrayList<>();
                        duplicateListMap.put(enhetPaaUniqueAgressoId.getId(), duplicates);
                    }
                    duplicates.add(e);
                }
            }
        );
        return duplicateListMap;
    }
}

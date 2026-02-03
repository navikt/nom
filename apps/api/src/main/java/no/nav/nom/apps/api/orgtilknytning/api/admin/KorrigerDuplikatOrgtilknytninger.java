package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

@Slf4j
@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/korrigerreduplikaterorgtilknytninger")
@PreAuthorize("hasAuthority('ADMIN')")
public class KorrigerDuplikatOrgtilknytninger {

    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public KorrigerDuplikatOrgtilknytninger(OrgtilknytningService orgtilknytningService,
                                            OrgtilknytningCommandService orgtilknytningCommandService
    ) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    @Operation(
        description = "Lag fjern eventer for duplikater. Kjør med False for kun dry-run",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    public String korrigere(@RequestParam(defaultValue = "false") boolean performUpdate) {
        // Group by navident
        Map<String, List<OrgtilknytningState>> groupedByNavident = orgtilknytningService.getAll().stream()
            .sorted(Comparator
                .comparing(OrgtilknytningState::orgEnhetId)
                .thenComparing(OrgtilknytningState::gyldigFom)
            )
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        Map<String, List<OrgtilknytningState>> duplikater = new HashMap<>();

        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavident.values().stream()
            .filter(group -> group.size() > 1)
            .forEach(otEr -> {
                OrgtilknytningState original = null;
                for (OrgtilknytningState ot : otEr) {
                    if (original != null) {
                        if (original.orgEnhetId().equals(ot.orgEnhetId())
                            && original.gyldigFom().equals(ot.gyldigFom())
                            && Objects.equals(original.gyldigTom(), ot.gyldigTom())
                        ) {
                            duplikater.putIfAbsent(original.fid(), new ArrayList<>());
                            duplikater.get(original.fid()).add(ot);
                        } else {
                            original = ot;
                        }
                    } else {
                        original = ot;
                    }
                }
            });

        StringBuffer str = new StringBuffer();
        JsonMapper mapper = new JsonMapper();
        duplikater.forEach( (originalFid, oter) -> oter.forEach(duplikat -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("Duplikate of", originalFid);
            node.put("For enhetId", duplikat.orgEnhetId());
            node.put("With period", duplikat.gyldigFom().toString() + " - " + requireNonNullElse(duplikat.gyldigTom(),  " "));
            FjernOrgtilknytning fjernOrgtilknytning =
                new FjernOrgtilknytning(
                    duplikat.navident(),
                    duplikat.orgEnhetId(),
                    duplikat.gyldigFom(),
                    duplikat.gyldigTom(),
                    new InputMetadata(
                        "Swagger",
                        "Korriger duplikate Orgtilknytninger",
                        node,
                        new Origination("nom-api", "nom-api")
                    ),
                    null
                );

            String eventStr =  "Fjern orgtilknytning event (navident: %s) for fid=%s, inputmeta=%s"
                .formatted(duplikat.navident(), duplikat.fid(), fjernOrgtilknytning.inputMetadata());
            str.append(eventStr).append("\n");
            log.info(eventStr);
            if (performUpdate) orgtilknytningCommandService.fjernOrgtilknytning(duplikat.fid(), fjernOrgtilknytning);
        }));

        return str.toString();
    }
}

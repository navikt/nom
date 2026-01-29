package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
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

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/korrigerreOrgtilknytingMedPeriodeUtoverEnhetensPeriode")
@PreAuthorize("hasAuthority('ADMIN')")
public class KorrigerOrgtilknytningerUtoverOrgenhetensPeriode {

    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public KorrigerOrgtilknytningerUtoverOrgenhetensPeriode(OrgenhetService orgenhetService, OrgtilknytningService orgtilknytningService,
                                                            OrgtilknytningCommandService orgtilknytningCommandService
    ) {
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    @Operation(
        description = "Lag avvikle eventer for orgtilknytinger som går utover orgenhetens periode. Kjør med False for kun dry-run",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping()
    public String korrigere(@RequestParam(defaultValue = "false") boolean performUpdate) {
        var alleOrgtilknytningState = orgtilknytningService.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        var aktiveTilknytningerUtoverEnhetPeriode = orgtilknytningerUtoverEnhetPeriode(aktiveOrgtilknytningState);

        JsonMapper mapper = new JsonMapper();
        StringBuffer str = new StringBuffer();
        aktiveTilknytningerUtoverEnhetPeriode.forEach( ot -> {
                var orgenhet = orgenhetService.getById(ot.orgEnhetId()).get();
                String otDescription = "Orgtilknytning %s for %s/%s med periode %s - %s".formatted(
                    ot.fid(),
                    ot.navident(),
                    ot.orgEnhetId(),
                    ot.gyldigFom(),
                    requireNonNullElse(ot.gyldigTom(), " ")
                );
                String orgenhetDescription = "Orgenhet %s %s med periode %s - %s".formatted(
                    orgenhet.getId(),
                    orgenhet.getNavn(),
                    orgenhet.getGyldigFom(),
                    requireNonNullElse(orgenhet.getGyldigTom(), " ")
                );

                ObjectNode node = mapper.createObjectNode();
                node.put("Orgiltknytning", otDescription);
                node.put("Orgenhet", orgenhetDescription);

                if (ot.gyldigFom().isAfter(orgenhet.getGyldigTom())) {
                    // Vi må fjerne orgtilknytningen, dne starteretter orgenheten slutter
                    FjernOrgtilknytning fjernOrgtilknytning =
                        new FjernOrgtilknytning(
                            ot.navident(),
                            ot.orgEnhetId(),
                            ot.gyldigFom(),
                            ot.gyldigTom(),
                            new InputMetadata(
                                "Swagger",
                                "Fjerner orgtilknytinger som går utover orgenhetens periode",
                                node,
                                new Origination("nom-api", "nom-api")
                            ),
                            null
                        );
                    str.append("Fjerner orgtilknytning %s".formatted(ot.fid())).append("\n");
                    str.append("\t").append(String.join(" og ", otDescription, orgenhetDescription)).append("\n");

                    log.info("Fjerner orgtilknytning for\n\t{} og {}", otDescription, orgenhetDescription);

                    if (performUpdate) orgtilknytningCommandService.fjernOrgtilknytning(ot.fid(), fjernOrgtilknytning);
                } else {
                    AvvikleOrgtilknytning avvikleOrgtilknytning =
                        new AvvikleOrgtilknytning(
                            ot.navident(),
                            ot.orgEnhetId(),
                            orgenhet.getGyldigTom(),
                            new InputMetadata(
                                "Swagger",
                                "Korriger orgtilknytinger som går utover orgenhetens periode",
                                node,
                                new Origination("nom-api", "nom-api")
                            ),
                            null
                        );
                    str.append("Avvikler orgtilknytning på dato %s".formatted(avvikleOrgtilknytning.gyldigTom())).append("\n");
                    str.append("\t").append(String.join(" og ", otDescription, orgenhetDescription)).append("\n");

                    log.info("Avvikler orgtilknytning på dato {} for\n\t{} og {}", avvikleOrgtilknytning.gyldigTom(), otDescription, orgenhetDescription);

                    if (performUpdate) orgtilknytningCommandService.avvikleOrgtilknytning(ot.fid(), avvikleOrgtilknytning);
                }
        });
        return str.toString();
    }

    private List<OrgtilknytningState> aktiveOrgtilknytninger(Collection<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream().filter( ot ->
            ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                (isNull(ot.gyldigTom()) || ot.gyldigTom().isAfter(LocalDate.now()))
        ).toList();
    }
    private List<OrgtilknytningState> orgtilknytningerUtoverEnhetPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
            .filter( ot -> {
                var orgEnhet = orgenhetService.getById(ot.orgEnhetId());
                if (nonNull(orgEnhet.get().getGyldigTom()) && orgEnhet.get().getGyldigTom().isBefore(ot.gyldigFom())) {
                    return true; // orgEnhet.gyldigTom < tilknytning.gyldigFom
                }
                if (nonNull(ot.gyldigTom()) && orgEnhet.get().getGyldigFom().isAfter(ot.gyldigTom())) {
                    return true; // orgEnhet.gyldigFom > tilknytning.gyldigTom
                }
                if (isNull(ot.gyldigTom()) && nonNull(orgEnhet.get().getGyldigTom())) {
                    return true; // orgEnhet avsluttet < tilknytning ikke avsluttet
                }
                return nonNull(ot.gyldigTom()) && nonNull(orgEnhet.get().getGyldigTom()) && orgEnhet.get().getGyldigTom().isBefore(ot.gyldigTom()); // orgEnhet avsluttet < tilknytning avsluttet
            } ).toList();
    }
}

package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.isNull;

@Slf4j
@RestController
@RequestMapping(name = "/api/admin/speilFysiskBrukerstedFraRessursTilOT")
@PreAuthorize("hasAuthority('ADMIN') || hasAuthority('LOCAL_NOM_API')")
@RequiredArgsConstructor
public class SpeilFysiskBrukerstedFraRessursTilOt {

    private final RessursService ressursService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    @Operation(
        description = "Migrer Primær fysisk brukersted fra RessursState til OrgtilknytningState",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public String speilFysiskBrukerstedFraRessursTilOt(@RequestParam boolean dryRun) {
        var alleAktiveRessurser = ressursService.findAllActiveRessurser();
        var alleAktiveOrgtilknytninger = orgtilknytningService.getByNavidenter(
            new HashSet<>(alleAktiveRessurser.keySet()),
            orgtilknytningState -> orgtilknytningState
        );

        InputMetadata inputMetadata = new InputMetadata(
            "REST",
            "speilFysiskBrukerstedFraRessursTilOT",
            null,
            new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
        );

        AtomicInteger counter = new AtomicInteger();
        StringBuilder stringBuilder = new StringBuilder();

        alleAktiveRessurser.forEach((navident, ressurserState) -> {
            if (isNull(ressurserState.primaryFysiskBrukersted()) || ressurserState.primaryFysiskBrukersted().isBlank()) {
                return;
            }

            alleAktiveOrgtilknytninger.get(navident).forEach(orgtilknytningState -> {
                var additionalFysiskBrukersteder = isNull(orgtilknytningState.additionalFysiskeBrukerstederNummer()) ?
                    new ArrayList<String>() : orgtilknytningState.additionalFysiskeBrukerstederNummer();
                if (!dryRun) {
                    try {
                        orgtilknytningCommandService.endreFysiskeBrukersted(
                            new EndreFysiskeBrukerstederCommand(
                                orgtilknytningState.fid(),
                                navident,
                                orgtilknytningState.orgEnhetId(),
                                ressurserState.primaryFysiskBrukersted(),
                                additionalFysiskBrukersteder,
                                inputMetadata.toBuilder()
                                    .inputInterfaceMethodArguments(
                                        Map.of(
                                            "primaryFysiskBrukersted", ressurserState.primaryFysiskBrukersted(),
                                            "fysiskeBrukersteder", additionalFysiskBrukersteder
                                        )
                                    )
                                    .build()
                            )
                        );
                    } catch (Exception e) {
                        stringBuilder.append(String.format("Navident %s fikk ikke overført fysiskBrukersted til OT\n", navident));
                        log.error("Feilet å sende event for å endre fysisk brukersted {}", e.getMessage(), e);
                    }
                } else {
                    counter.getAndIncrement();
                }
            });
        });
        if (dryRun) {
            return stringBuilder.append(String.format("Fant %s aktive OT som fikk ny fysisk brukersted", counter.get())).toString();
        } else {
            return stringBuilder.toString();
        }
    }
}

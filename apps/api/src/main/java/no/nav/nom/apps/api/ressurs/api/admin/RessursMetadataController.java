package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.ressurs.eventsourcing.metadata.RessursMetadataRepository;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.api.graphql.RessursWithMetadataResult;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import no.nav.nom.libs.gql.MappingUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Tag(name = "Admin for ressurs metadata")
@Slf4j
@RestController
@RequestMapping("/api/admin/ressurs/")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursMetadataController {

    private final RessursStateRepository ressursStateRepository;
    private final RessursMetadataRepository ressursMetadataRepository;

    public RessursMetadataController(RessursStateRepository ressursStateRepository, RessursMetadataRepository ressursMetadataRepository) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursMetadataRepository = ressursMetadataRepository;
    }

    @Operation(
        description = "Hent ressurser metadata",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("ressurserMetadata")
    public List<RessursWithMetadataResult> recoveryEventSource() {
        return this.ressurserWithMetadata();
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception e) {
        log.error("Feil skjedde under henting av ressurser med metadata, med følgende feilmelding {}", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder()
                .message("Server error: %s".formatted(e.getMessage()))
                .build());
    }

    public List<RessursWithMetadataResult> ressurserWithMetadata() {
        record Result(
            String id,
            RessursState ressurs
        ) {
            public boolean isEmpty() {
                return ressurs == null;
            }

            public static Result from(RessursState ressurs) {
                return from(ressurs.navident(), ressurs);
            }

            public static Result from(String id, RessursState ressurs) {
                return new Result(id, ressurs);
            }
        }

        Stream<Result> result = ressursStateRepository.getAll().stream().map(Result::from);

        return result.map(res ->
            new RessursWithMetadataResult(
                res.id,
                MappingUtils.toRessurs(res.ressurs),
                ressursMetadataRepository.findOpprettetAvByNavident(res.id),
                ressursMetadataRepository.findOpprettetDatoByNavident(res.id),
                ressursMetadataRepository.findEndretAvByNavident(res.id),
                ressursMetadataRepository.findEndretDatoByNavident(res.id),
                ressursMetadataRepository.findEndringEventByNavident(res.id)
            )
        ).collect(Collectors.toList());
    }


}





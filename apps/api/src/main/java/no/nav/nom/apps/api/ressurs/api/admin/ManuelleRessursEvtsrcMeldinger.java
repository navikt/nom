package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.app.AvvikleRessursRequest;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.apps.api.ressurs.masterdata.NomMasteredService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.flywaydb.core.internal.util.HtmlUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleRessursEvtsrcMeldinger {

    private final RessursCommandService ressursCommandService;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;

    public ManuelleRessursEvtsrcMeldinger(
        RessursCommandService ressursCommandService,
        RessursStateRepository ressursStateRepository, NomMastered nomMastered
    ) {
        this.ressursCommandService = ressursCommandService;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
    }


    @Operation(
        description = "Lag en slett ressurs Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PostMapping(value = "/lagSlettRessursMelding")
    public ResponseEntity<String> lagSlettRessursMelding(
        @Parameter(description = "FID for ressursen som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("lagSlettRessursMelding : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " + HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke slettes da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
                    ressursState.get().navident(),
                    ressursState.get().personident()
                ));
        }

        ressursCommandService.slettRessursCommand(
            new SlettRessursCommand(fid),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "lagSlettRessursMelding",
                fid,
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s og personident %s. Slett event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().personident()
            ));
    }

    @Operation(
        description = "Lag en Oppdater personident for ressurs Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PostMapping(value = "/oppdaterPersonidentForRessurs")
    public ResponseEntity<String> oppdaterPersonidentForRessurs(
        @Parameter(description = "FID for ressursen som skal få ny personident") @RequestParam String fid,
        @Parameter(description = "Ny personident (fnr)") @RequestParam String nyPersonident,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("oppdaterPersonidentForRessurs : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        if (!validPersonident(nyPersonident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Personident er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " + HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
                    ressursState.get().navident(),
                    ressursState.get().personident()
                ));
        }

        ressursCommandService.oppdaterPersonidentCommand(
            new OppdaterPersonidentCommand(fid, nyPersonident),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "oppdaterPersonidentForRessurs",
                fid,
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s og personident %s. Endre personident til %s event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().personident(),
                HtmlUtils.htmlEncode(nyPersonident)
            ));
    }

    @Operation(
        description = "Lag en Oppdater Personalia for ressurs Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PostMapping(value = "/oppdaterPersonaliaForRessurs")
    public ResponseEntity<String> oppdaterPersonidentForRessurs(
        @Parameter(description = "FID for ressursen som skal få endret personalia") @RequestParam String fid,
        @Parameter(description = "Fornavn") @RequestParam String fornavn,
        @Parameter(description = "Mellomnavn") @RequestParam(required = false) String mellomnavn,
        @Parameter(description = "Etternavn") @RequestParam(required = false) String etternavn,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("oppdaterPersonaliaForRessurs : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " + HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s med navn %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
                    ressursState.get().navident(),
                    ressursState.get().visningsnavn().toString()
                ));
        }

        if (mellomnavn != null && mellomnavn.isBlank()) {
            mellomnavn = null;
        }

        ressursCommandService.oppdaterPersonaliaEvent(
            new OppdaterPersonaliaCommand(
                HtmlUtils.htmlEncode(fid),
                new Visningsnavn(fornavn, Optional.ofNullable(mellomnavn), etternavn)
            ),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "oppdaterPersonaliaForRessurs",
                fid,
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s med navn %s. Endre personalia event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().visningsnavn().toString()
            ));
    }

    @Operation(
        description = "Lag mastre navident Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @PostMapping(value = "/merkNavidentSomMastret")
    public ResponseEntity<String> merkNavidentSomMastret(
        @Parameter(description = "Navident som skal merkes som mastret") @RequestParam String navident,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (ressursStateRepository.findByNavident(navident).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Navidenten finnes ikke");
        }

        if (nomMastered.isMastered(navident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Navidenten er allerede mastret i NOM");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med navident %s er funnet og er ikke allerede Mastret i NOM. Men event vil ikke sendes for å mastre da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        }

        var ok = nomMastered.markRessursAsMasteredInNom(navident);

        if (ok) {
            return ResponseEntity.ok()
                .body("Ressurs med navident %s er nå mastret i NOM".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Feil ved forsøk på å merke navident %s som mastret".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        }
    }

    @Operation(
        description = "Lag Eventsrc melding for å fjerne mastret i NOM merking",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @PostMapping(value = "/avmerkNavidentSomMastret")
    public ResponseEntity<String> avmerkNavidentSomMastret(
        @Parameter(description = "Navident som skal få mastret i NOM merking") @RequestParam String navident,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (ressursStateRepository.findByNavident(navident).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Navidenten finnes ikke");
        }

        if (!nomMastered.isMastered(navident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Navidenten er ikke mastret i NOM");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med navident %s er funnet og er Mastret i NOM. Men event vil ikke sendes for å av-merke da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        }

        var ok = nomMastered.unmarkRessursAsMasteredInNom(navident);

        if (ok) {
            return ResponseEntity.ok()
                .body("Ressurs med navident %s er nå av-merket som mastret i NOM".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Feil ved forsøk på å av-merke navident %s som mastret".formatted(
                    HtmlUtils.htmlEncode(navident)
                ));
        }
    }

    @PostMapping(value = "/avvikleRessurs")
    @Operation(
        description = "Lag en Ressurs Sluttet Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    public ResponseEntity<String> avsluttRessurs(
        @Parameter(description = "NAVIDENT for ressursen som skal avsluttes") @RequestParam String navident,
        @Parameter(description = "Sluttdato for ressursen") @RequestParam(required = false) LocalDate localDate,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {

        var ressursState = ressursStateRepository.findByNavident(navident);

        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Navidenten finnes ikke");
        }

        if (!nomMastered.isMastered(navident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Navidenten er ikke mastret i NOM");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s med navn %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(ressursState.get().fid()),
                    ressursState.get().navident(),
                    ressursState.get().visningsnavn().toString()
                ));
        }

        var metaData = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "avvikleRessurs",
            new AvvikleRessursRequest(localDate),
            new Origination(
                getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        ressursCommandService.avsluttRessursEvent(
            new OpphorRessursCommand(ressursState.get().fid(), localDate),
            metaData
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s med navn %s. Avvikle ressurs event er nå sendt".formatted(
                HtmlUtils.htmlEncode(ressursState.get().fid()),
                ressursState.get().navident(),
                ressursState.get().visningsnavn().toString()
            ));
    }


    @PostMapping(value = "/oppdatereSluttDatoRessurs")
    @Operation(
        description = "Lag en RessursSluttdatoOppdatertEvent melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    public ResponseEntity<String> oppdatereSluttDatoRessurs(
        @Parameter(description = "NAVIDENT for ressursen") @RequestParam String navident,
        @Parameter(description = "Sluttdato for ressursen som skal oppdateres") @RequestParam(required = false) LocalDate sluttDato,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ){
        var ressursState = ressursStateRepository.findByNavident(navident);

        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Navidenten finnes ikke");
        }

        if (!nomMastered.isMastered(navident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Navidenten er ikke mastret i NOM");
        }

        if (isNull(sluttDato)){
            log.warn("Oppdateret sluttdato for gitt ressurs: {} er tom!", navident);
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s med navn %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(ressursState.get().fid()),
                    ressursState.get().navident(),
                    ressursState.get().visningsnavn().toString()
                ));
        }

        var metaData = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "oppdatereSluttDatoRessurs",
            new AvvikleRessursRequest(sluttDato),
            new Origination(
                getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        ressursCommandService.oppdaterRessursSluttdatoEvent(
            new OppdaterSluttdatoCommand(ressursState.get().fid(),
                   isNull(sluttDato)? Optional.empty() : Optional.of(sluttDato), metaData)
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s med navn %s. RessursSluttdatoOppdatertEvent er nå sendt".formatted(
                HtmlUtils.htmlEncode(ressursState.get().fid()),
                ressursState.get().navident(),
                ressursState.get().visningsnavn().toString()
            ));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }

    private boolean validPersonident(String fid) {
        return fid.matches("[0-9]+");
    }
}

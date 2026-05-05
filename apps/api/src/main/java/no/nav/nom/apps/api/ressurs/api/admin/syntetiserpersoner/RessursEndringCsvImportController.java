package no.nav.nom.apps.api.ressurs.api.admin.syntetiserpersoner;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Tag(name = "Admin CSV import for ressurs-endringer")
@RestController
@RequestMapping("/api/admin/syntetiserpersoner")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursEndringCsvImportController {

    private static final int DRY_RUN_PREVIEW_LIMIT = 100;

    private final GcsBucketCsvReader gcsBucketCsvReader;
    private final RessursCommandService ressursCommandService;
    private final RessursStateRepository ressursStateRepository;

    public RessursEndringCsvImportController(
        GcsBucketCsvReader gcsBucketCsvReader,
        RessursCommandService ressursCommandService,
        RessursStateRepository ressursStateRepository
    ) {
        this.gcsBucketCsvReader = gcsBucketCsvReader;
        this.ressursCommandService = ressursCommandService;
        this.ressursStateRepository = ressursStateRepository;
    }

    @Operation(
        description = "Les CSV-fil fra GCS bucket og oppdater navn og/eller personident for ressurser. "
            + "Uten confirm=CONFIRM vises de første 100 radene og en oppsummering uten å sende events.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/ressursEndringer")
    public ResponseEntity<String> importRessursEndringer(
        @Parameter(description = "Filnavn i GCS bucket") @RequestParam String fileName,
        @Parameter(description = "Skriv CONFIRM for å utføre endringer. Uten CONFIRM vises kun forhåndsvisning.") @RequestParam(required = false) String confirm
    ) {
        boolean isConfirm = "CONFIRM".equals(confirm);
        log.info("importRessursEndringer called, confirm={}", isConfirm);

        CsvParseResult parseResult;
        try {
            parseResult = gcsBucketCsvReader.readCsvFromBucket(fileName);
        } catch (Exception e) {
            log.error("Feil ved lesing av CSV fra bucket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Feil ved lesing av CSV fra bucket. Se server-logg for detaljer.");
        }

        var rows = parseResult.rows();
        var parseWarnings = parseResult.warnings();

        if (rows.isEmpty() && parseWarnings.isEmpty()) {
            return ResponseEntity.ok("CSV-filen er tom (ingen datarader funnet).");
        }

        if (!isConfirm || !parseWarnings.isEmpty()) {
            return ResponseEntity.ok(buildDryRunResponse(rows, parseWarnings));
        }

        log.info("Executing ressursEndringer for {} rows", rows.size());
        return ResponseEntity.ok(executeChanges(rows, parseWarnings));
    }

    @Operation(
        description = "Les CSV-fil fra GCS bucket med ressurs-FID-er og send slett-event for hver. "
            + "Uten confirm=CONFIRM vises forhåndsvisning uten å sende events.",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping("/slettRessurser")
    public ResponseEntity<String> importSlettRessurser(
        @Parameter(description = "Filnavn i GCS bucket (CSV med kun FID-kolonne)") @RequestParam String fileName,
        @Parameter(description = "Skriv CONFIRM for å utføre sletting. Uten CONFIRM vises kun forhåndsvisning.") @RequestParam(required = false) String confirm
    ) {
        boolean isConfirm = "CONFIRM".equals(confirm);
        log.info("importSlettRessurser called, confirm={}", isConfirm);

        CsvParseResult parseResult;
        try {
            parseResult = gcsBucketCsvReader.readFidOnlyCsvFromBucket(fileName);
        } catch (Exception e) {
            log.error("Feil ved lesing av CSV fra bucket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Feil ved lesing av CSV fra bucket. Se server-logg for detaljer.");
        }

        var rows = parseResult.rows();
        var parseWarnings = parseResult.warnings();

        if (rows.isEmpty() && parseWarnings.isEmpty()) {
            return ResponseEntity.ok("CSV-filen er tom (ingen datarader funnet).");
        }

        if (!isConfirm || !parseWarnings.isEmpty()) {
            return ResponseEntity.ok(buildFidDryRunResponse(rows, parseWarnings));
        }

        log.info("Executing slettRessurser for {} rows", rows.size());
        return ResponseEntity.ok(executeDeletes(rows, parseWarnings));
    }

    private String buildDryRunResponse(List<RessursEndringCsvRow> rows, List<String> parseWarnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DRY RUN - Ingen endringer utført ===\n");
        sb.append("Totalt antall rader i filen: ").append(rows.size()).append("\n");

        long withNavn = rows.stream().filter(RessursEndringCsvRow::hasNavn).count();
        long withPersonident = rows.stream().filter(RessursEndringCsvRow::hasPersonident).count();
        sb.append("Rader med navneendring: ").append(withNavn).append("\n");
        sb.append("Rader med personident-endring: ").append(withPersonident).append("\n\n");

        sb.append("--- Forhåndsvisning (maks ").append(DRY_RUN_PREVIEW_LIMIT).append(" rader) ---\n");
        sb.append(String.format("%-30s %-20s %-20s %-15s%n", "FID", "Fornavn", "Etternavn", "Personident"));
        sb.append("-".repeat(85)).append("\n");

        int limit = Math.min(rows.size(), DRY_RUN_PREVIEW_LIMIT);
        for (int i = 0; i < limit; i++) {
            var row = rows.get(i);
            sb.append(String.format("%-30s %-20s %-20s %-15s%n",
                row.fid(),
                row.fornavn() != null ? row.fornavn() : "",
                row.etternavn() != null ? row.etternavn() : "",
                row.personident() != null ? row.personident() : ""
            ));
        }

        if (rows.size() > DRY_RUN_PREVIEW_LIMIT) {
            sb.append("\n... og ").append(rows.size() - DRY_RUN_PREVIEW_LIMIT).append(" rader til.\n");
        }

        appendWarnings(sb, parseWarnings);
        return sb.toString();
    }

    private String executeChanges(List<RessursEndringCsvRow> rows, List<String> parseWarnings) {
        List<String> executionWarnings = new ArrayList<>();
        int personaliaUpdated = 0;
        int personidentUpdated = 0;
        List<String> skippedFids = new ArrayList<>();
        List<String> unchangedNavn = new ArrayList<>();
        List<String> unchangedPersonident = new ArrayList<>();

        for (var row : rows) {
            var ressursState = ressursStateRepository.findByFid(row.fid());
            if (ressursState.isEmpty()) {
                skippedFids.add(row.fid());
                continue;
            }

            var state = ressursState.get();

            InputMetadata inputMetadata = new InputMetadata(
                "/api/admin/syntetiserpersoner",
                "ressursEndringer",
                row.fid(),
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin Syntetisering ressurser"
                )
            );

            if (row.hasNavn()) {
                var eksisterende = state.visningsnavn();
                boolean navnEndret = eksisterende == null
                    || !Objects.equals(row.fornavn(), eksisterende.fornavn())
                    || !Objects.equals(row.etternavn(), eksisterende.etternavn());

                if (navnEndret) {
                    try {
                        ressursCommandService.oppdaterPersonaliaEvent(
                            new OppdaterPersonaliaCommand(
                                row.fid(),
                                new Visningsnavn(row.fornavn(), Optional.empty(), row.etternavn())
                            ),
                            inputMetadata
                        );
                        personaliaUpdated++;
                    } catch (Exception e) {
                        log.error("Feil ved oppdatering av navn for FID={}", row.fid(), e);
                        executionWarnings.add("FEIL ved oppdatering av navn for FID=" + row.fid());
                    }
                } else {
                    unchangedNavn.add(row.fid());
                }
            }

            if (row.hasPersonident()) {
                boolean personidentEndret = !Objects.equals(row.personident(), state.personident());

                if (personidentEndret) {
                    try {
                        ressursCommandService.oppdaterPersonidentCommand(
                            new OppdaterPersonidentCommand(row.fid(), row.personident()),
                            inputMetadata
                        );
                        personidentUpdated++;
                    } catch (Exception e) {
                        log.error("Feil ved oppdatering av personident for FID={}", row.fid(), e);
                        executionWarnings.add("FEIL ved oppdatering av personident for FID=" + row.fid());
                    }
                } else {
                    unchangedPersonident.add(row.fid());
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Ferdig. Personalia oppdatert: %d, Personident oppdatert: %d, Skipped: %d, Uendret navn: %d, Uendret personident: %d (av totalt %d rader)\n"
            .formatted(personaliaUpdated, personidentUpdated, skippedFids.size(), unchangedNavn.size(), unchangedPersonident.size(), rows.size()));

        if (!skippedFids.isEmpty()) {
            sb.append("\n--- Ikke funnet i state (").append(skippedFids.size()).append(") ---\n");
            for (String fid : skippedFids) {
                sb.append("  ").append(fid).append("\n");
            }
        }

        if (!unchangedNavn.isEmpty()) {
            sb.append("\n--- Uendret navn (").append(unchangedNavn.size()).append(") ---\n");
            for (String fid : unchangedNavn) {
                sb.append("  ").append(fid).append("\n");
            }
        }

        if (!unchangedPersonident.isEmpty()) {
            sb.append("\n--- Uendret personident (").append(unchangedPersonident.size()).append(") ---\n");
            for (String fid : unchangedPersonident) {
                sb.append("  ").append(fid).append("\n");
            }
        }

        List<String> allWarnings = new ArrayList<>(parseWarnings);
        allWarnings.addAll(executionWarnings);
        appendWarnings(sb, allWarnings);

        if (!allWarnings.isEmpty()) {
            log.warn("ressursEndringer ferdig med {} advarsler", allWarnings.size());
        }

        return sb.toString();
    }

    private void appendWarnings(StringBuilder sb, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        sb.append("\n--- Advarsler (").append(warnings.size()).append(") ---\n");
        for (String warning : warnings) {
            sb.append("  ⚠ ").append(warning).append("\n");
        }
    }

    private String buildFidDryRunResponse(List<RessursEndringCsvRow> rows, List<String> parseWarnings) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DRY RUN - Ingen endringer utført ===\n");
        sb.append("Totalt antall FID-er i filen: ").append(rows.size()).append("\n\n");

        sb.append("--- Forhåndsvisning (maks ").append(DRY_RUN_PREVIEW_LIMIT).append(" rader) ---\n");

        int limit = Math.min(rows.size(), DRY_RUN_PREVIEW_LIMIT);
        for (int i = 0; i < limit; i++) {
            sb.append("  ").append(rows.get(i).fid()).append("\n");
        }

        if (rows.size() > DRY_RUN_PREVIEW_LIMIT) {
            sb.append("\n... og ").append(rows.size() - DRY_RUN_PREVIEW_LIMIT).append(" rader til.\n");
        }

        appendWarnings(sb, parseWarnings);
        return sb.toString();
    }

    private String executeDeletes(List<RessursEndringCsvRow> rows, List<String> parseWarnings) {
        List<String> executionWarnings = new ArrayList<>();
        int deleted = 0;
        List<String> skippedFids = new ArrayList<>();

        for (var row : rows) {
            var ressursState = ressursStateRepository.findByFid(row.fid());
            if (ressursState.isEmpty()) {
                skippedFids.add(row.fid());
                continue;
            }

            InputMetadata inputMetadata = new InputMetadata(
                "/api/admin/syntetiserpersoner",
                "slettRessurser",
                row.fid(),
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin Syntetisering ressurser"
                )
            );

            try {
                ressursCommandService.slettRessursCommand(
                    new SlettRessursCommand(row.fid()),
                    inputMetadata
                );
                deleted++;
            } catch (Exception e) {
                log.error("Feil ved sletting av FID={}", row.fid(), e);
                executionWarnings.add("FEIL ved sletting av FID=" + row.fid());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Ferdig. Slettet: %d, Skipped: %d (av totalt %d rader)\n"
            .formatted(deleted, skippedFids.size(), rows.size()));

        if (!skippedFids.isEmpty()) {
            sb.append("\n--- Ikke funnet i state (").append(skippedFids.size()).append(") ---\n");
            for (String fid : skippedFids) {
                sb.append("  ").append(fid).append("\n");
            }
        }

        List<String> allWarnings = new ArrayList<>(parseWarnings);
        allWarnings.addAll(executionWarnings);
        appendWarnings(sb, allWarnings);

        if (!allWarnings.isEmpty()) {
            log.warn("slettRessurser ferdig med {} advarsler", allWarnings.size());
        }

        return sb.toString();
    }
}

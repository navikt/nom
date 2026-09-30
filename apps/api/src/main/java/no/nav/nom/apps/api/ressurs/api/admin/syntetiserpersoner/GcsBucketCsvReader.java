package no.nav.nom.apps.api.ressurs.api.admin.syntetiserpersoner;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GcsBucketCsvReader {

    @Value("${app.google-storage-bucket-name.ressurs-endringer}")
    private String bucketName;

    @Value("${nais.cluster.name:local}")
    private String gsProjectId;

    /**
     * Reads a CSV file from the configured GCS bucket and parses all rows.
     * Expects header row: fid,fornavn,etternavn,personident
     * Personident column is optional (may be empty).
     */
    public CsvParseResult readCsvFromBucket(String objectName) {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        List<RessursEndringCsvRow> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8));

            String line;
            boolean headerSkipped = false;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                parseRow(line, lineNumber, warnings).ifPresent(rows::add);
            }

            log.info("Read {} rows from bucket ({} warnings)", rows.size(), warnings.size());

        } catch (Exception e) {
            log.error("Failed to read CSV from bucket: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read CSV from GCS bucket", e);
        }

        return new CsvParseResult(rows, warnings);
    }

    public CsvParseResult readFidOnlyCsvFromBucket(String objectName) {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        List<RessursEndringCsvRow> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, objectName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8));

            String line;
            boolean headerSkipped = false;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                if (line.isBlank()) {
                    continue;
                }

                String fid = line.trim().replace("\"", "");
                if (fid.isBlank()) {
                    warnings.add("Linje %d: Tom FID, hoppet over".formatted(lineNumber));
                    continue;
                }
                if (fid.contains(",") || fid.contains(" ")) {
                    warnings.add("Linje %d: Ugyldig FID-format (inneholder komma eller mellomrom), hoppet over".formatted(lineNumber));
                    continue;
                }

                rows.add(new RessursEndringCsvRow(fid, null, null, null));
            }

            log.info("Read {} FID-er from bucket ({} warnings)", rows.size(), warnings.size());

        } catch (Exception e) {
            log.error("Failed to read FID CSV from bucket: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read CSV from GCS bucket", e);
        }

        return new CsvParseResult(rows, warnings);
    }

    private java.util.Optional<RessursEndringCsvRow> parseRow(String line, int lineNumber, List<String> warnings) {
        var columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (columns.length < 3) {
            warnings.add("Linje %d: Ugyldig rad (færre enn 3 kolonner), hoppet over".formatted(lineNumber));
            return java.util.Optional.empty();
        }

        String fid = columns[0].trim().replace("\"", "");
        String fornavn = columns[1].trim().replace("\"", "");
        String etternavn = columns[2].trim().replace("\"", "");
        String personident = columns.length > 3 ? columns[3].trim().replace("\"", "") : "";

        if (fid.isBlank()) {
            warnings.add("Linje %d: Tom FID, hoppet over".formatted(lineNumber));
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new RessursEndringCsvRow(
            fid,
            fornavn.isBlank() ? null : fornavn,
            etternavn.isBlank() ? null : etternavn,
            personident.isBlank() ? null : personident
        ));
    }
}

package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Sektor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ImportRemedyDataFile {
    @Value("${app.google-storage-bucket.name}")
    private String bucketName;

    @Value("${app.google-storage-bucket.remedy-data}")
    private String fileName;

    @Value("${nais.cluster.name:local}")
    private String gsProjectId;

    public List<ImportedRemedyUser> importfromBucket() {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();

        Page<Blob> blobs = storage.list(bucketName);
        for (Blob blob : blobs.iterateAll()) {
            log.info("Blob i bucket {}: {}", bucketName, blob.getName());
        }
        if (blobs.streamValues().noneMatch(blob -> blob.getName().equals(fileName))) {
            log.error("Fant ikke filen {} i bucket {} - vil ikke laste data", fileName, bucketName);
            return List.of();
        }

        try (ReadChannel reader = storage.reader(BlobId.of(bucketName, fileName))) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, StandardCharsets.UTF_8));
            
            List<ImportedRemedyUser> remedyUsers = readRemedyData(br);
            return remedyUsers;
        } catch (Exception e) {
            log.error("Klarer ikke å lese fra bucket: ", e);
            throw new RuntimeException("Klarer ikke å lese fra bucket", e);
        }
    }


    public List<ImportedRemedyUser> readRemedyData(BufferedReader reader) throws IOException {
        List<ImportedRemedyUser> remedyUsers = new ArrayList<>();

        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                if (!isHeaderLine(line)) {
                    parseAndValidateLine(line).ifPresent(remedyUsers::add);
                }
            }
            log.info("Leste {} Remedy fra bucket {}", remedyUsers.size(), bucketName);
        } catch (Exception e) {
            log.error("Klarer ikke å lese fra bucket for line\n\t{}", line);
            throw new RuntimeException("Klarer ikke å lese fra bucket", e);
        }

        return remedyUsers;
    }


    // NAV_ID;FORNAVN;ETTERNAVN;ENHETS_ID;ENHET;STATUS;I_PERMISJON_;T1;REG__DATO;BRUKER_ID;SEKTOR;UTLØPSDATO;UTLØPSDATO_GOSYS_EGEN_ANSATT;UTLØPSDATO_GOSYS_KODE_7
    private Optional<ImportedRemedyUser> parseAndValidateLine(String line) {
        log.debug("Parsing line: {}", line);
        var kolonner = line.split(";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); // Split på semikolon, ignorer komma i "

        if ("<null>".equals(kolonner[0])) {
            return Optional.empty();
        }

        var navident = kolonner[0];
        var fornavn = kolonner[1];
        var etternavn = kolonner[2];
        var enhetsId = kolonner[3];
        var enhetsNavn = kolonner[4];
        var aktiv = "0".equals(kolonner[5]);
        var permisjon = "0".equals(kolonner[6]);
        var sluttDato = "<null>".equals(kolonner[7]) ? null : Instant.ofEpochMilli(Long.parseLong(kolonner[7])*1000).atZone(ZoneId.systemDefault()).toLocalDate();
        var sektor = switch (kolonner[10]) {
            case "0" -> Sektor.NAV_STATLIG;
            case "1" -> Sektor.NAV_KOMMUNAL;
            case "2" -> Sektor.EKSTERN;
            default -> null;
        };
        var utlopsDato = "<null>".equals(kolonner[11]) ? null : Instant.ofEpochMilli(Long.parseLong(kolonner[11])*1000).atZone(ZoneId.systemDefault()).toLocalDate();

        return Optional.of(new ImportedRemedyUser(
                navident,
                fornavn,
                etternavn,
                enhetsId,
                enhetsNavn,
                sektor,
                aktiv,
                permisjon,
                sluttDato,
                utlopsDato
        ));
    }

    private boolean isHeaderLine(String line) {
        return line.startsWith("NAV_ID");
    }
}


package no.nav.nom.apps.navident.job;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;

@Service
@Slf4j
public class ImportNavIdenter {

    @Value("app.gs-bucket-name") private String bucketName;
    @Value("nais.cluster.name") private String gsProjectId;

    public String fromBucketObject(String objectName) {
        Storage storage = StorageOptions.newBuilder().setProjectId(gsProjectId).build().getService();
        Blob blob = storage.get(bucketName, objectName);
        try (ReadChannel reader = blob.reader()) {
            BufferedReader br = new BufferedReader(Channels.newReader(reader, "UTF-8"));
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 10; i++) {
                sb.append(br.readLine());
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("Unable to read input", e);
            return "Unable to read. " + e.getMessage();
        }
    }

    public void importTestNavIdent(String importLine) {

    }

}

package no.nav.nom.apps.azure;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.odataerrors.ODataError;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AzureAdService {

    private final Logger log = LoggerFactory.getLogger(AzureAdService.class);

    private final GraphServiceClient graphServiceClient;

    private final Counter countPictureFetch;

    public AzureAdService(Environment environment, MeterRegistry meterRegistry) {
        var azProps = new AzureProperties(environment);
        var clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(azProps.clientId)
            .clientSecret(azProps.clientSecret)
            .tenantId(azProps.tenantId)
            .build();

        this.graphServiceClient = new GraphServiceClient(clientSecretCredential);

        this.countPictureFetch = meterRegistry.counter("requests.picture_fetched.count");

    }

    public Optional<byte[]> fetchProfilePictureByNavIdent(String navIdent) {
        countPictureFetch.increment();
        String userId = lookupUserByNavIdent(navIdent);
        var pictureBytes = userId == null ? null : lookupUserProfilePicture(userId);
        log.debug("Fetch of profile picture for {}: {}", navIdent, pictureBytes == null ? "failed" : "success");
        return Optional.ofNullable(pictureBytes);
    }

    private String lookupUserByNavIdent(String navIdent) {
        try {

            var res = this.graphServiceClient
                .users()
                .get(requestConfiguration -> {
                    requestConfiguration.queryParameters.select = new String[]{"id"};
                    requestConfiguration.queryParameters.filter = "mailNickname eq '" + navIdent + "'";
                })
                .getValue();
            if (res != null && res.size() != 1) {
                log.debug("Did not find single user for navIdent {} ({})", navIdent, res.size());
                return null;
            }
            return res.getFirst().getId();
        } catch (Exception e) {
            log.debug("Lookup of user-id by navident failed");
            return null;
        }

    }


    private byte[] lookupUserProfilePicture(String azureUserId) {
        try {
            var photoContents = this.graphServiceClient
                .users()
                .byUserId(azureUserId)
                .photo()
                .content()
                .get();

            if(photoContents == null){
                return null;
            }

            var bytes = photoContents.readAllBytes();
            photoContents.close();

            return bytes;
        } catch (ODataError oDataError) {
            if (oDataError.getError() != null && "ImageNotFound".equals(oDataError.getError().getCode())) {
                log.info("Azure fetch: Image not found for user id {}", azureUserId);
            } else {
                log.warn("Azure fetch: OData error for user id {}", azureUserId);
            }
            return null;
        }
        catch (Exception e) {
            // More granular logging of errors could be added if necessary
            return null;
        }

    }

    private static class AzureProperties{
        public final String clientId;
        public final String clientSecret;
        public final String tenantId;

        AzureProperties(Environment environment) {
            try {
                this.clientId = environment.getRequiredProperty("AZURE_APP_CLIENT_ID");
                this.clientSecret = environment.getRequiredProperty("AZURE_APP_CLIENT_SECRET");
                this.tenantId = environment.getRequiredProperty("AZURE_APP_TENANT_ID");
            } catch (Exception e) {
                throw new RuntimeException("Missing one or more 'AZURE_APP_...' environment variables ");
            }
        }
    }
}

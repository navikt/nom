package no.nav.nom.apps.azure;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AzureAdService {

    private final Logger log = LoggerFactory.getLogger(AzureAdService.class);

    private final GraphServiceClient graphServiceClient;

    private final LoadingCache<String, byte[]> pictureCache;

    public AzureAdService(Environment environment) {
        var azProps = new AzureProperties(environment);
        var clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(azProps.clientId)
            .clientSecret(azProps.clientSecret)
            .tenantId(azProps.tenantId)
            .build();

        this.graphServiceClient = new GraphServiceClient(clientSecretCredential);

        this.pictureCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(12))
                .maximumSize(10000).build(this::lookupProfilePictureByNavIdent);
    }

    private byte[] lookupProfilePictureByNavIdent(String navIdent) {
        String userId = lookupUserByNavIdent(navIdent);
        var pictureBytes = userId == null ? null : lookupUserProfilePicture(userId);
        log.info("Fetch of profile picture for {}: {}", navIdent, pictureBytes == null ? "failed" : "success");
        return pictureBytes;
    }

    public Optional<byte[]> profilePictureByNavIdent(String navIdent){
        var out = pictureCache.get(navIdent);
        return Optional.ofNullable(out);
    }

    public void refreshPictureCache(String navIdent) {
        this.pictureCache.refresh(navIdent);
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
                log.info("Did not find single user for navIdent {} ({})", navIdent, res.size());
                return null;
            }
            return res.getFirst().getId();
        } catch (Exception e) {
            log.error("Lookup of user-id by navident failed");
            return null;
        }

    }


    private byte[] lookupUserProfilePicture(String id) {
        try {
            var photoContents = this.graphServiceClient
                .users()
                .byUserId(id)
                .photo()
                .content()
                .get();

            if(photoContents == null){
                return null;
            }

            var bytes = photoContents.readAllBytes();
            photoContents.close();

            return bytes;
        } catch (Exception e) {
            return null;
        }

        /*
            ODataError =>
//            if (e.getError() != null && "ImageNotFound".equals(e.getError().getCode())) {
                    ...
                }else { ...}
            ApiException, IOException, NullPointerException => { ... log }
         */

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

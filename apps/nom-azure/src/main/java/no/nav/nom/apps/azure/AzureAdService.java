package no.nav.nom.apps.azure;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AzureAdService {


    private final GraphServiceClient graphServiceClient;

    private final Logger log = LoggerFactory.getLogger(AzureAdService.class);

    public AzureAdService(Environment environment) {
        var azProps = new AzureProperties(environment);
        var clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(azProps.clientId)
            .clientSecret(azProps.clientSecret)
            .tenantId(azProps.tenantId)
            .build();

        this.graphServiceClient = new GraphServiceClient(clientSecretCredential);

    }

    public Optional<byte[]> lookupProfilePictureByNavIdent(String navIdent) {
        String userId = lookupUserByNavIdent(navIdent);
        return lookupUserProfilePicture(userId);
    }


    private String lookupUserByNavIdent(String navIdent) {
                var res = this.graphServiceClient
                    .users()
                    .get(requestConfiguration -> {
                        requestConfiguration.queryParameters.select = new String[] {"id"};
                        requestConfiguration.queryParameters.filter = "mailNickname eq '" + navIdent + "'";
                    })
                    .getValue();
                if (res != null && res.size() != 1) {
                    log.info("Did not find single user for navIdent {} ({})", navIdent, res.size());
                    return null;
                }
                return res.getFirst().getId();
    }


    private Optional<byte[]> lookupUserProfilePicture(String id) {
        try {
            var photoContents = this.graphServiceClient
                .users()
                .byUserId(id)
                .photo()
                .content()
                .get();

            if(photoContents == null){
                return Optional.empty();
            }

            var bytes = photoContents.readAllBytes();
            photoContents.close();

            return Optional.of(bytes);
        } catch (Exception e) {
            return Optional.empty();
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
            this.clientId = environment.getRequiredProperty("AZURE_APP_CLIENT_ID");
            this.clientSecret = environment.getRequiredProperty("AZURE_APP_CLIENT_SECRET");
            this.tenantId = environment.getRequiredProperty("AZURE_APP_TENANT_ID");
        }
    }
}

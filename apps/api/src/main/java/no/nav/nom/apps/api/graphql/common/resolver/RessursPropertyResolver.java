package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.LoaderUtils;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader.AD_RESSURS_DATALOADER;


@Component
public class RessursPropertyResolver implements GraphQLResolver<Ressurs> {


    private final boolean sendDummydata;

    public RessursPropertyResolver(Environment env){
        var naisClientId = env.getProperty("NAIS_CLUSTER_NAME");
        // Sliter med at AD fungerer dårlig i dev-miljø.
        sendDummydata = naisClientId != null && naisClientId.startsWith("dev");
    }

    private CompletableFuture<DataFetcherResult<String>> respondWithDummydata(String data){
        var o = DataFetcherResult.<String>newResult().data(data).build();
        return CompletableFuture.completedFuture(o);
    }

    public CompletableFuture<DataFetcherResult<String>> epost(Ressurs ressurs, DataFetchingEnvironment env) {
        if(sendDummydata){
            return respondWithDummydata("dummy-epost@i-dev." + ressurs.getNavIdent());
        }
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getEmail);
    }

    public CompletableFuture<DataFetcherResult<String>> visningsnavn(Ressurs ressurs, DataFetchingEnvironment env) {
        if(sendDummydata){
            return respondWithDummydata("dummy-visningsnavn-i-dev" + ressurs.getNavIdent());
        }
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getDisplayName);
    }

    public CompletableFuture<DataFetcherResult<String>> visningsNavn(Ressurs ressurs, DataFetchingEnvironment env) {
        return visningsnavn(ressurs, env);
    }

    public CompletableFuture<DataFetcherResult<String>> fornavn(Ressurs ressurs, DataFetchingEnvironment env) {
        if(sendDummydata){
            return respondWithDummydata("dummy-fornavn-i-dev-" + ressurs.getNavIdent());
        }
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getGivenName);
    }

    public CompletableFuture<DataFetcherResult<String>> etternavn(Ressurs ressurs, DataFetchingEnvironment env) {
        if(sendDummydata){
            return respondWithDummydata("dummy-etternnavn-i-dev-" + ressurs.getNavIdent());
        }
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getSurname);
    }

}

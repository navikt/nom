package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.LoaderUtils;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

import static no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader.AD_RESSURS_DATALOADER;


@Component
public class RessursPropertyResolver implements GraphQLResolver<Ressurs> {

    public CompletableFuture<DataFetcherResult<String>> epost(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getEmail);
    }

    public CompletableFuture<DataFetcherResult<String>> visningsNavn(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getDisplayName);
    }

    public CompletableFuture<DataFetcherResult<String>> fornavn(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getGivenName);
    }

    public CompletableFuture<DataFetcherResult<String>> etternavn(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, AD_RESSURS_DATALOADER, ressurs.getNavIdent(), AdRessurs::getSurname);
    }

}

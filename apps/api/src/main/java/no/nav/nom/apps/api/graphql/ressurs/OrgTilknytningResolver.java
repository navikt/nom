package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.LoaderUtils;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class OrgTilknytningResolver implements GraphQLResolver<Ressurs> {

    public CompletableFuture<DataFetcherResult<List<RessursOrgTilknytning>>> orgTilknytning(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, OrgTilknytningBatchLoader.ORGTILKNYTNING_DATALOADER, ressurs.getNavIdent());
    }

    public CompletableFuture<DataFetcherResult<List<RessursOrgTilknytning>>> koblinger(Ressurs ressurs, DataFetchingEnvironment env) {
        return orgTilknytning(ressurs, env);
    }

}

package no.nav.nom.apps.api.graphql.common;

//import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningBatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import static no.nav.nom.apps.api.graphql.common.loader.ActiveDirectoryRessursBatchLoader.AD_RESSURS_DATALOADER;
import static no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader.PERSON_DATALOADER;
import static no.nav.nom.apps.api.graphql.ressurs.OrgTilknytningBatchLoader.ORGTILKNYTNING_DATALOADER;

@Component
@RequiredArgsConstructor
class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {

    private static final int MAX_BATCH_SIZE = 100;
    private final PersonBatchLoader personBatchLoader;
    private final OrgTilknytningBatchLoader orgTilknytningBatchLoader;
    private final ActiveDirectoryRessursBatchLoader adRessursBatchLoader;
    private final AppProperties appProperties;

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new CustomGraphQLContext(SecurityContextHolder.getContext().getAuthentication(), buildDataLoaderRegistry(), appProperties.isSandbox());
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLContext build() {
        return null;
    }

    private DataLoaderRegistry buildDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
        DataLoaderOptions options = new DataLoaderOptions().setMaxBatchSize(MAX_BATCH_SIZE);
        val personDataloader = DataLoader.newMappedDataLoader(personBatchLoader, options);
        val adRessursDataloader = DataLoader.newMappedDataLoader(adRessursBatchLoader, options);
        val orgTilknytningDataLoader = DataLoaderFactory.newMappedDataLoader(orgTilknytningBatchLoader, options);

        dataLoaderRegistry.register(PERSON_DATALOADER, personDataloader);
        dataLoaderRegistry.register(AD_RESSURS_DATALOADER, adRessursDataloader);
        dataLoaderRegistry.register(ORGTILKNYTNING_DATALOADER, orgTilknytningDataLoader);
        return dataLoaderRegistry;
    }
}

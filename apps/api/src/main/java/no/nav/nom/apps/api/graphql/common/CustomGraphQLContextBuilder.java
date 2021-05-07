package no.nav.nom.apps.api.graphql.common;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.loader.EpostBatchLoader;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import static no.nav.nom.apps.api.graphql.common.loader.EpostBatchLoader.EPOST_DATALOADER;
import static no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader.PERSON_DATALOADER;

@Component
@RequiredArgsConstructor
class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {

    private final PersonBatchLoader personBatchLoader;
    private final EpostBatchLoader epostBatchLoader;

    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new CustomGraphQLContext(SecurityContextHolder.getContext().getAuthentication(), buildDataLoaderRegistry());
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
        val personDataloader = DataLoader.newMappedDataLoader(personBatchLoader);
        val epostDataloader = DataLoader.newMappedDataLoader(epostBatchLoader);

        dataLoaderRegistry.register(PERSON_DATALOADER, personDataloader);
        dataLoaderRegistry.register(EPOST_DATALOADER, epostDataloader);
        return dataLoaderRegistry;
    }
}

package no.nav.nom.apps.api.graphql.common;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

import static no.nav.nom.apps.api.graphql.common.PersonBatchLoader.PERSON_DATALOADER;

@Component
@RequiredArgsConstructor
class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {

    private final PersonBatchLoader personBatchLoader;

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
        val dataloader = DataLoader.newMappedDataLoader(personBatchLoader);

        dataLoaderRegistry.register(PERSON_DATALOADER, dataloader);
        return dataLoaderRegistry;
    }
}

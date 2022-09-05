package no.nav.nom.apps.api.graphql.common;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Map;
import java.util.Optional;
import javax.security.auth.Subject;

@Getter
@RequiredArgsConstructor
public class CustomGraphQLContext implements GraphQLKickstartContext {
    private final Authentication authentication;
    private final DataLoaderRegistry dataLoaderRegistry;
    private final boolean sandbox;

    @Override
    public Optional<Subject> getSubject() {
        return Optional.empty();
    }

    @Override
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    @Override
    public Map<Object, Object> getMapOfContext() {
        return null;
    }

    public boolean isAuthenticated() {
        return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated();
    }
}

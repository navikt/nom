package no.nav.nom.apps.api.graphql.common;

import graphql.kickstart.execution.context.GraphQLContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import javax.security.auth.Subject;

@Getter
@RequiredArgsConstructor
public class CustomGraphQLContext implements GraphQLContext {
    private final Authentication authentication;
    private final DataLoaderRegistry dataLoaderRegistry;

    @Override
    public Optional<Subject> getSubject() {
        return Optional.empty();
    }

    @Override
    public DataLoaderRegistry getDataLoaderRegistry() {
        return dataLoaderRegistry;
    }

    public boolean isAuthenticated() {
        return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated();
    }
}

package no.nav.nom.apps.api.graphql;

import graphql.kickstart.execution.context.GraphQLContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoaderRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.security.auth.Subject;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class CustomGraphQLContext implements GraphQLContext {

    private final Authentication authentication;

    @Override
    public Optional<Subject> getSubject() {
        return Optional.empty();
    }

    @Override
    public Optional<DataLoaderRegistry> getDataLoaderRegistry() {
        return Optional.empty();
    }

    public boolean isAuthenticated(){
        return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated();
    }
}

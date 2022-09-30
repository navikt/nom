//package no.nav.nom.apps.api.graphql.common;
//
//import graphql.kickstart.execution.context.DefaultGraphQLContext;
//import graphql.kickstart.execution.context.GraphQLKickstartContext;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.dataloader.DataLoaderRegistry;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//
//import java.util.Map;
//import java.util.Optional;
//import javax.security.auth.Subject;
//
//@Getter
//@RequiredArgsConstructor
//public class CustomGraphQLContext extends DefaultGraphQLContext {
//    private final Authentication authentication;
//    private final DataLoaderRegistry dataLoaderRegistry;
//    private final boolean sandbox;
//
//
//    public boolean isAuthenticated() {
//        return authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated();
//    }
//}

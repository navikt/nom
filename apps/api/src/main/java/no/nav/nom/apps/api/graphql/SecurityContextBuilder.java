package no.nav.nom.apps.api.graphql;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

@Component
class SecurityContextBuilder implements GraphQLServletContextBuilder {
    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return new CustomGraphQLContext(SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLContext build() {
        return null;
    }
}

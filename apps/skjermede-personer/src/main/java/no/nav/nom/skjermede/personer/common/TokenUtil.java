package no.nav.nom.skjermede.personer.common;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TokenUtil {

    public static final String ISSUER = "selvbetjening";

    private final TokenValidationContextHolder contextHolder;

    public TokenUtil(final TokenValidationContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    public boolean isAuthenticated() {
        return getSubject() != null;
    }

    public boolean isAuthorized(final String fnr) {
        return isAuthenticated() && fnr.equals(getSubject());
    }

    public String getSubject() {
        return Optional.ofNullable(claimSet())
                .map(JwtTokenClaims::getSubject)
                .orElse(null);
    }

    private JwtTokenClaims claimSet() {
        return Optional.ofNullable(contextHolder.getTokenValidationContext())
                .map(c -> c.getClaims(ISSUER))
                .orElse(null);
    }
}

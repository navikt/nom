package no.nav.nom.skjermede.personer.common.security;

import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class TokenUtil {

    public static final String ISSUER = "selvbetjening";

    private final TokenValidationContextHolder contextHolder;

    public TokenUtil(final TokenValidationContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    public boolean isAuthorized() {
        return getSubject() != null;
    }

    public Date getExpirationDate() {
        return Optional.ofNullable(claimSet())
                .map(JwtTokenClaims::getExpirationTime)
                .orElse(null);
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

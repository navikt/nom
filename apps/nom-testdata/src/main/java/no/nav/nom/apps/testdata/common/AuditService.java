package no.nav.nom.apps.testdata.common;

import no.nav.nom.apps.testdata.common.repository.Audit;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    public Audit.Endring endring() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String bruker = "nom-testdata";
        if (authentication instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            Object navident = jwt.getToken().getClaims().get("NAVident");
            if (navident instanceof String ident && !ident.isBlank()) {
                bruker = ident.strip();
            }
        }
        return new Audit.Endring(LocalDateTime.now(), bruker);
    }
}

package no.nav.nom.apps.proxy.api;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
abstract class AbstractController {

    @Value("${spring.security.oauth2.resourceserver.jwt.subjects}")
    private List<String> AUTHORIZED_SUBJECTS;
    private final String INVALID_SUBJECT = "Subject tokenet %s er ikke godkjent";

    void assertJwt(Jwt jwt) {
        val tokenSubject = jwt.getClaim("sub");
        if (tokenSubject == null || !AUTHORIZED_SUBJECTS.contains(tokenSubject)) {
            String msg = INVALID_SUBJECT.formatted(tokenSubject);
            log.warn(msg);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
        }
    }
}

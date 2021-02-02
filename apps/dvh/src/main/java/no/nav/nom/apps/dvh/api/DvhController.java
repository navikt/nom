package no.nav.nom.apps.dvh.api;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.dvh.common.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class DvhController {

    private final String INVALID_SUBJECT = "Subject tokenet gjelder for er ikke godkjent";

    @Value("${spring.security.oauth2.resourceserver.jwt.subjects}")
    private final List<String> AUTHORIZED_SUBJECTS;

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    @GetMapping("/stillinger")
    public String getStillinger(@AuthenticationPrincipal Jwt jwt) {
        val tokenSubject = jwt.getClaim("sub");
        if (tokenSubject == null || !AUTHORIZED_SUBJECTS.contains(tokenSubject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_SUBJECT);
        }
        return restTemplate.exchange(appProperties.getDvh().getStilling().getUrl(),
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/ressurser")
    public String getRessurser(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(required = false) Integer bucketNum_in) {
        val tokenSubject = jwt.getClaim("sub");
        if (tokenSubject == null || !AUTHORIZED_SUBJECTS.contains(tokenSubject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_SUBJECT);
        }
        val requestParamStr = (bucketNum_in != null) ?
                "?bucketNum_in=" + bucketNum_in
                : "";

        return restTemplate.exchange(appProperties.getDvh().getRessurser().getUrl() + requestParamStr,
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/enheter")
    public String getEnheter(@AuthenticationPrincipal Jwt jwt) {
        val tokenSubject = jwt.getClaim("sub");
        if (tokenSubject == null || !AUTHORIZED_SUBJECTS.contains(tokenSubject)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, INVALID_SUBJECT);
        }
        return restTemplate.exchange(appProperties.getDvh().getEnheter().getUrl(),
                HttpMethod.GET, null, String.class).getBody();
    }

}

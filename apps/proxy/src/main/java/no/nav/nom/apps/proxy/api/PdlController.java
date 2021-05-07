package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.proxy.common.AppProperties;
import no.nav.nom.apps.proxy.common.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class PdlController extends AbstractController {

    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;

    @Autowired //Lombok constructor injection funker ikke med qualifier.
    private RestTemplate restTemplate;

    @PostMapping("/pdl")
    public ResponseEntity<String> hentPersonBolk(@AuthenticationPrincipal Jwt jwt, @RequestBody String body, @RequestHeader("Tema") String tema) {
        assertJwt(jwt);

        val headers = new HttpHeaders();
        val token = tokenRepository.getStsToken();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.add("Nav-Consumer-Token", "Bearer " + token);
        headers.add("Tema", tema);

        val result = restTemplate.exchange(appProperties.getPdl().getBaseUrl() + "/graphql", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        return ResponseEntity.status(HttpStatus.OK).body(result.getBody());
    }
}

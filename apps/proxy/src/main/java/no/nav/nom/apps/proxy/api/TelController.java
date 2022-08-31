package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class TelController {

    private final RestTemplate restTemplate;

    private final AppProperties appProperties;

    @GetMapping("/tel")
    public String getTelv1(@AuthenticationPrincipal Jwt jwt) {
        var request = RequestEntity
                .get(appProperties.getTel().getUrl())
                .header("Authorization", "Basic " + HttpHeaders.encodeBasicAuth(
                        appProperties.getTel().getTelUsername(),
                        appProperties.getTel().getTelPassword(),
                        Charset.defaultCharset()
                ))
                .build();

        var out = restTemplate.exchange(request, String.class).getBody();
        return out;
    }
}

package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/proxy/unit4")
@RequiredArgsConstructor
public class Unit4Controller extends AbstractController{
    public static final String API_EMPLOYEES_PATH = "/agrt01-web-api/v1/employees";
    private final RestTemplate restTemplate;

    private final AppProperties appProperties;

    @PostMapping("/ressurs")
    public String postRessurs(@AuthenticationPrincipal Jwt jwt, @RequestBody String body) {
        assertJwt(jwt);
        log.info("Mottatt /proxy/unit4/ressurs POST request");

        var request = RequestEntity
                .post(appProperties.getUnit4().getBaseUrl() + API_EMPLOYEES_PATH)
                .headers(h -> h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password() ))
                .body(body);

        return restTemplate.exchange(request, String.class).getBody(); // nullptr ?
    }

    @GetMapping("/**")
    public String unit4GetProxy(HttpServletRequest request){
        var target = appProperties.getUnit4().getBaseUrl() + request.getRequestURI().replaceFirst("/proxy/unit4", "");
        target = request.getQueryString() == null || request.getQueryString().isEmpty() ? target : target + "?" + request.getQueryString();
        log.info("Mottatt forespørsel med path: "+request.getRequestURI()+", ruter til: "+target);

        var proxiedRequest = RequestEntity
                .get(appProperties.getUnit4().getBaseUrl())
                .headers(h -> h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password() ))
                .build();

        return restTemplate.exchange(proxiedRequest, String.class).getBody(); // nullptr ?
    }


}



package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class DvhController extends AbstractController {

    @Autowired //Lombok constructor injection funker ikke med qualifier.
    @Qualifier("restTemplateDvh")
    private RestTemplate restTemplateDvh;

    private final AppProperties appProperties;

    private final String PROXY_PATH = "/dvh";

    @GetMapping(PROXY_PATH+"/**")
    public String dvhProxyEndpoint(HttpServletRequest request){
        val target = appProperties.getDvh().getBaseUrl() + request.getRequestURI().replaceFirst("/proxy"+PROXY_PATH, "");
        log.info("Mottatt forespørsel med path: "+request.getRequestURI()+", ruter til: "+target);
        val response = restTemplateDvh.exchange(target,
                HttpMethod.GET, null, String.class).getBody();

        log.info("Forwarding response: "+ response);

        return response;
    }

    @GetMapping("/stillinger")
    public String getStillinger(@AuthenticationPrincipal Jwt jwt) {
        assertJwt(jwt);

        return restTemplateDvh.exchange(appProperties.getDvh().getStilling().getUrl(),
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/ressurser")
    public String getRessurser(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Integer bucketNum_in) {
        assertJwt(jwt);

        val requestParamStr = (bucketNum_in != null) ?
                "?bucketNum_in=" + bucketNum_in
                : "";

        return restTemplateDvh.exchange(appProperties.getDvh().getRessurser().getUrl() + requestParamStr,
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/enheter")
    public String getEnheter(@AuthenticationPrincipal Jwt jwt) {
        assertJwt(jwt);

        return restTemplateDvh.exchange(appProperties.getDvh().getEnheter().getUrl(),
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/enheter/v2")
    public String getEnheterV2(@AuthenticationPrincipal Jwt jwt) {
        assertJwt(jwt);

        return restTemplateDvh.exchange(appProperties.getDvh().getEnheter().getUrlV2(),
                HttpMethod.GET, null, String.class).getBody();
    }

    @GetMapping("/dvhTlf")
    public String getTlf(@AuthenticationPrincipal Jwt jwt) {
        assertJwt(jwt);
        return restTemplateDvh.exchange(appProperties.getDvh().getTelDvh().getUrl(),
                HttpMethod.GET, null, String.class).getBody();

    }

}

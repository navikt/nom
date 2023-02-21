package no.nav.nom.apps.proxy.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

import static no.nav.nom.apps.proxy.common.CommonConfig.SECURE_LOGS;


@Slf4j
@RestController
@RequestMapping(value = "/proxy/unit4", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class Unit4Controller {
    //public static final String API_EMPLOYEES_PATH = "/agrt01-web-api/v1/employees";
    private final RestTemplate restTemplate;

    private final AppProperties appProperties;

    @PostMapping("/ressurs")
    public ResponseEntity<String> postRessurs(@AuthenticationPrincipal Jwt jwt, @RequestBody String body) {
        log.info("Mottatt /proxy/unit4/ressurs POST request");
        log.debug(SECURE_LOGS, "POST /proxy/unit4/ressurs payload: {}", body);

        var request = RequestEntity
                .post(appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath())
                .headers(h -> {
                            h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password());
                            h.setContentType(MediaType.APPLICATION_JSON);
                        }
                )
                .body(body);
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientResponseException restClientResponseException){
            response = ResponseEntity.status(restClientResponseException.getRawStatusCode())
                    .body(restClientResponseException.getMessage());
        }
        return response;
    }

    @PatchMapping("/ressurs/{personId}")
    public ResponseEntity<String> postRessurs(@PathVariable String personId, @AuthenticationPrincipal Jwt jwt, @RequestBody String body) {
        log.info("Mottatt /proxy/unit4/ressurs PATCH request");
        log.debug(SECURE_LOGS, "PATCH /proxy/unit4/ressurs/{} payload: {}", personId, body);


        var request = RequestEntity
                .patch(appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath() + "/" + personId)
                .headers(h -> {
                            h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password());
                            //Content-type i request må være application/json-patch+json
                            h.add(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
                }
                )
                .body(body);
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientResponseException restClientResponseException){
            response = ResponseEntity.status(restClientResponseException.getRawStatusCode())
                    .body(restClientResponseException.getMessage());
        }
        return response;
    }

    @DeleteMapping("/ressurs/{personId}")
    public ResponseEntity<String> deleteRessurs(@PathVariable String personId, @AuthenticationPrincipal Jwt jwt) {
        log.info("Mottatt /proxy/unit4/ressurs DELETE request");
        log.debug(SECURE_LOGS, "DELETE /proxy/unit4/ressurs/{}", personId);


        var request = RequestEntity
            .delete(appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath() + "/" + personId)
            .headers(h -> {
                    h.setBasicAuth(appProperties.getUnit4().getUnit4Username(), appProperties.getUnit4().getUnit4Password());
                    //Content-type i request må være application/json-patch+json
                    h.add(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
                }
            ).build();
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientResponseException restClientResponseException){
            response = ResponseEntity.status(restClientResponseException.getRawStatusCode())
                .body(restClientResponseException.getMessage());
        }
        return response;
    }

    @GetMapping("/**")
    public String unit4GetProxy(HttpServletRequest request){
        var target = appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getApiPath() + request.getRequestURI().replaceFirst("/proxy/unit4", "");
        target = request.getQueryString() == null || request.getQueryString().isEmpty() ? target : target + "?" + request.getQueryString();
        log.info("Mottatt forespørsel med path: "+request.getRequestURI()+", ruter til: "+target);

        var proxiedRequest = RequestEntity
                .get(target)
                .headers(h -> {
                    h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password());
                    h.add(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
                })
                .build();

        return restTemplate.exchange(proxiedRequest, String.class).getBody(); // nullptr ?
    }


}



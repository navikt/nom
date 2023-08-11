package no.nav.nom.apps.proxy.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
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
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static no.nav.nom.apps.proxy.common.CommonConfig.SECURE_LOGS;


@Slf4j
@RestController
@RequestMapping(value = "/proxy/unit4", produces = MediaType.APPLICATION_JSON_VALUE)
public class Unit4Controller {
    private final RestTemplate restTemplate;

    private final AppProperties appProperties;

    private final WebClient webClient;

    public Unit4Controller(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.webClient = WebClient.builder()
            .baseUrl(appProperties.getUnit4().getBaseUrl())
            .filter(ExchangeFilterFunctions
                .basicAuthentication(appProperties.getUnit4().getUnit4Username(), appProperties.getUnit4().getUnit4Password()))
            .build();
    }

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
            log.error("Error in Unit4 post request", restClientResponseException);
            response = ResponseEntity.status(restClientResponseException.getRawStatusCode())
                    .body(restClientResponseException.getMessage());
        }
        return response;
    }

    @PatchMapping("/ressurs/{personId}")
    public ResponseEntity<String> patchRessurs(@PathVariable String personId, @AuthenticationPrincipal Jwt jwt, @RequestBody String body) {
        String url = appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath() + "/" + personId;
        log.info("Mottatt /proxy/unit4/ressurs PATCH request -> {}", url);
        log.debug(SECURE_LOGS, "PATCH /proxy/unit4/ressurs/{} payload: {}", personId, body);

        var request = RequestEntity
                .patch(url)
                .headers(h -> {
                            h.setBasicAuth(appProperties.getUnit4().getUnit4Username(),appProperties.getUnit4().getUnit4Password());
                            //Content-type i request må være application/json-patch+json
                            h.add(HttpHeaders.CONTENT_TYPE, "application/json-patch+json");
                            h.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .body(body);
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientResponseException restClientResponseException){
            log.error("Error in Unit4 patch request", restClientResponseException);
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
                    h.setContentType(MediaType.APPLICATION_JSON);
                }
            ).build();
        ResponseEntity<String> response;
        try{
            response = restTemplate.exchange(request, String.class);
        } catch (RestClientResponseException restClientResponseException){
            log.error("Error in Unit4 delete request", restClientResponseException);
            response = ResponseEntity.status(restClientResponseException.getRawStatusCode())
                .body(restClientResponseException.getMessage());
        }
        return response;
    }

    @GetMapping("/ressurs/{personId}")
    public ResponseEntity<String> getRessurs(@PathVariable String personId, @AuthenticationPrincipal Jwt jwt) {
        log.info("Mottatt /proxy/unit4/{personId} ressurs GET request");
        log.debug(SECURE_LOGS, "GET /proxy/unit4/ressurs/{}", personId);

        try {
            String response = webClient.get()
                .uri(appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath() + "/" + personId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return ResponseEntity.ok(response);
        } catch (WebClientResponseException ex) {
            log.error("Error in Unit4 get ressurs/{personId} request:", ex);

            String body = ex.getResponseBodyAsString();
            HttpStatusCode status = ex.getStatusCode();
            HttpHeaders headers = ex.getHeaders();

            return ResponseEntity.status(status)
                .headers(headers)
                .body(body);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/**")
    public ResponseEntity<String> unit4GetProxy(HttpServletRequest request){
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

        return restTemplate.exchange(proxiedRequest, String.class);
    }

}



package no.nav.nom.apps.proxy.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.proxy.common.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

import static no.nav.nom.apps.proxy.common.CommonConfig.SECURE_LOGS;


@Slf4j
@RestController
@RequestMapping(value = "/proxy/unit4", produces = MediaType.APPLICATION_JSON_VALUE)
public class Unit4Controller {

    private final AppProperties appProperties;

    private final String unit4EmployeesUrl;

    private final WebClient webClient;

    private final UriBuilderFactory uriBuilderFactory;

    public Unit4Controller(AppProperties appProperties, WebClient.Builder webClientBuilder) {
        this.appProperties = appProperties;
        this.webClient = webClientBuilder
            .filter(ExchangeFilterFunctions
                .basicAuthentication(appProperties.getUnit4().getUnit4Username(), appProperties.getUnit4().getUnit4Password()))
            .build();
        this.uriBuilderFactory = new DefaultUriBuilderFactory(appProperties.getUnit4().getBaseUrl());
        this.unit4EmployeesUrl = appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath();
    }

    @PostMapping("/ressurs")
    public ResponseEntity<String> postRessurs(@RequestBody String body) {
        log.info("Mottatt /proxy/unit4/ressurs POST request");
        log.debug(SECURE_LOGS, "POST /proxy/unit4/ressurs payload: {}", body);

        try {
            return webClient
                .post()
                .uri(unit4EmployeesUrl)
                .body(Mono.just(body), String.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class)
                .block();

        } catch (WebClientResponseException ex) {
            log.error("Error in Unit4 POST request:", ex);

            return getErrorResponseEntity(ex);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage());
        }
    }

    @PatchMapping("/ressurs/{personId}")
    public ResponseEntity<String> patchRessurs(@PathVariable String personId, @RequestBody String body) {
        String url = unit4EmployeesUrl + "/" + personId;
        log.info("Mottatt /proxy/unit4/ressurs PATCH request -> {}", url);
        log.debug(SECURE_LOGS, "PATCH /proxy/unit4/ressurs/{} payload: {}", personId, body);

        try {
            return webClient
                .patch()
                .uri(url)
                .contentType(MediaType.valueOf("application/json-patch+json"))  //Content-type i request må være application/json-patch+json
                .body(Mono.just(body), String.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class)
                .block();

        } catch (WebClientResponseException ex) {
            log.error("Error in Unit4 PATCH request:", ex);

            return getErrorResponseEntity(ex);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage());
        }
    }

    @DeleteMapping("/ressurs/{personId}")
    public ResponseEntity<String> deleteRessurs(@PathVariable String personId) {
        log.info("Mottatt /proxy/unit4/ressurs DELETE request");
        log.debug(SECURE_LOGS, "DELETE /proxy/unit4/ressurs/{}", personId);

        try {
            return webClient
                .delete()
                .uri(unit4EmployeesUrl + "/" + personId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class)
                .block();

        } catch (WebClientResponseException ex) {
            log.error("Error in Unit4 DELETE request:", ex);

            return getErrorResponseEntity(ex);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/ressurs/{personId}")
    public ResponseEntity<String> getRessurs(@PathVariable String personId) {
        log.info("Mottatt /proxy/unit4/{personId} ressurs GET request");
        log.debug(SECURE_LOGS, "GET /proxy/unit4/ressurs/{}", personId);

        try {
            String response = webClient.get()
                .uri(unit4EmployeesUrl + "/" + personId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return ResponseEntity.ok(response);
        } catch (WebClientResponseException ex) {
            log.error("Error in Unit4 GET ressurs/{personId} request:", ex);

            return getErrorResponseEntity(ex);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage());
        }
    }

    @GetMapping("/**")
    public ResponseEntity<String> unit4GetProxy(HttpServletRequest request) {
        String targetPath = appProperties.getUnit4().getApiPath() + request.getRequestURI().replaceFirst("/proxy/unit4", "");

        URI uri = uriBuilderFactory
            .uriString(targetPath)
            .query(request.getQueryString())
            .build();

        log.info("Mottatt GET-forespørsel med path: " + request.getRequestURI() + ", ruter til: " + uri);

        return webClient
            .get()
            .uri(uri)
            .retrieve()
            .toEntity(String.class)
            .block();
    }

    private static ResponseEntity<String> getErrorResponseEntity(WebClientResponseException ex) {
        String errorBody = ex.getResponseBodyAsString();
        HttpStatusCode status = ex.getStatusCode();
        HttpHeaders headers = ex.getHeaders();

        return ResponseEntity.status(status)
            .headers(headers)
            .body(errorBody);
    }
}



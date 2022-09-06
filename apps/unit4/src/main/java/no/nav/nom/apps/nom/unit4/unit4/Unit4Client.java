package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.apps.nom.unit4.TokenRepository;
import no.nav.nom.apps.nom.unit4.input.NomRessursDTO;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class Unit4Client {

    private final WebClient webClient = WebClient.create();
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final TokenRepository tokenRepository;

    private final AppProperties appProperties;

    private final static String RESSURS_POST_ENDPOINT = "/proxy/unit4/ressurs";

    @SneakyThrows
    public String createRessurs(NomRessursDTO nomRessursDTO) {
        val requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setBasicAuth(appProperties.getUnit4().getApiUsername(), appProperties.getUnit4().getApiPassword());
        val body = objectMapper.writeValueAsString(new Unit4RessursDto(nomRessursDTO));

        val request = new HttpEntity<>(body, requestHeaders);

        return restTemplate.exchange(appProperties.getUnit4().getBaseUrl() + appProperties.getUnit4().getEmployeesPath(), HttpMethod.POST, request, String.class).getBody();
    }

    public void createRessursByProxy(Unit4RessursDto unit4RessursDto) throws Unit4Exception, JsonProcessingException {
        val requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.getProxy().getAadClientId()).serialize());

        val body = objectMapper.writeValueAsString(unit4RessursDto);

        val request = new HttpEntity<>(body, requestHeaders);

        try {
            val response = webClient.post()
                    .uri(appProperties.getProxy().getBaseUrl() + RESSURS_POST_ENDPOINT)
                    .headers(httpHeaders -> {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.getProxy().getAadClientId()).serialize());
                    })
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block();

        } catch (WebClientResponseException wce) {
            HttpStatus httpStatus = wce.getStatusCode();

            logIfErrorMessageInBody(wce.getResponseBodyAsString());

            if (httpStatus == HttpStatus.CREATED) {
                return; // OK
            } else if (httpStatus == HttpStatus.CONFLICT) {
                throw new Unit4ConflictException("Ressurs finnes i Unit4. (HttpStatus.CONFLICT)");
            } else if (httpStatus.is4xxClientError()) {
                throw new Unit4RequestException("Unit4 melder klient feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else if (httpStatus.is5xxServerError()) {
                throw new Unit4ServerException("Unit4 melder server feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else {
                throw new Unit4UnknownException("Ukjent feil fra Unit4. HttpStatus: %s".formatted(httpStatus.toString()));
            }
        } catch (WebClientException wce) {
            log.error("Unit4 createRessurs request error", wce);
            throw new Unit4UnknownException("Annen ukjent feil ved request til Unit4");
        }
    }

    private void logIfErrorMessageInBody(String body) {
        if (body != null && body.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode bodyNode = null;
            try {
                bodyNode = mapper.readTree(body);
            } catch (JsonProcessingException e) {
                log.warn("Could not parse Unit4 response body when looking for 'notificationMessages'");
            }
            bodyNode.findValues("$..notificationMessages").forEach(node -> log.error("Unit4 notification: {}", node.asText()));
        }
    }
}
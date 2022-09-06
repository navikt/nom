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

@Service
@RequiredArgsConstructor
@Slf4j
public class Unit4Client {

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
            ResponseEntity<JsonNode> response = restTemplate.exchange(appProperties.getProxy().getBaseUrl() + RESSURS_POST_ENDPOINT, HttpMethod.POST, request, JsonNode.class);

            if (response.hasBody()) logIfErrorMessageInBody(response.getBody());
            HttpStatus httpStatus = response.getStatusCode();
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
        } catch (RestClientException rce) {
            throw new Unit4UnknownException(""); // TODO:
        }
    }

    private void logIfErrorMessageInBody(JsonNode body) {
        // TODO: Parse body, logg Feilmelding
        body.findValues("$..notificationMessages").forEach(node -> log.error("Unit4 notification: {}", node.asText()));
    }
}
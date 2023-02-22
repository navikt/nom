package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.apps.nom.unit4.TokenRepository;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4PatchRequest;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static no.nav.nom.apps.nom.unit4.AppConfig.SECURE_LOGS;

@Service
@RequiredArgsConstructor
@Slf4j
public class Unit4Client {

    private final static MediaType UNIT4_MEDIETYPE_JSON_PATCH = new MediaType("application", "json-patch+json");
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper;

    private final TokenRepository tokenRepository;

    private final AppProperties appProperties;

    private final static String RESSURS_ENDPOINT = "/proxy/unit4/ressurs";

    public JsonNode findRessurs(String personId) throws Unit4Exception{
        val url = appProperties.proxy().baseUrl() + RESSURS_ENDPOINT + "/" + personId;
        log.debug("Get unit4 ressurs [{}]:\n{}", url);

        try {
            JsonNode response =
                webClient.get()
                    .uri(appProperties.proxy().baseUrl() + RESSURS_ENDPOINT)
                    .headers(httpHeaders -> {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());
                        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            log.debug(SECURE_LOGS, "Response fra find unit4 ressurs:\n{}", response.toPrettyString());
            return response;
        } catch (WebClientResponseException wce) {
            HttpStatus httpStatus = wce.getStatusCode();
            log.debug("Error finding unit4 ressurs", wce);

            logIfErrorMessageInBody(wce.getResponseBodyAsString(StandardCharsets.UTF_8));

            if (httpStatus == HttpStatus.NOT_FOUND || httpStatus == HttpStatus.GONE) {
                throw new Unit4NotFoundException("Ressurs finnes ikke i Unit4.");
            } else if (httpStatus.is4xxClientError()) {
                throw new Unit4RequestException("Unit4 melder klient feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else if (httpStatus.is5xxServerError()) {
                throw new Unit4ServerException("Unit4 melder server feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else {
                throw new Unit4UnknownException("Ukjent feil fra Unit4. HttpStatus: %s".formatted(httpStatus.toString()));
            }
        } catch (WebClientException wce) {
            log.warn("Unit4 findRessurs request error", wce);
            throw new Unit4UnknownException("Annen ukjent feil ved request til Unit4", wce);
        }
    }


    public JsonNode createRessurs(Unit4RessursDto unit4RessursDto) throws Unit4Exception, JsonProcessingException {
        val body = objectMapper.writeValueAsString(unit4RessursDto);
        log.debug(SECURE_LOGS, "Create unit4 ressurs:\n{}", body);
        try {
            JsonNode response =
                webClient.post()
                    .uri(appProperties.proxy().baseUrl() + RESSURS_ENDPOINT)
                    .headers(httpHeaders -> {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());
                        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block()
                    .getBody();


            log.debug(SECURE_LOGS, "Response fra create unit4 ressurs:\n{}", response.toPrettyString());
            return response;
        } catch (WebClientResponseException wce) {
            HttpStatus httpStatus = wce.getStatusCode();
            log.error(SECURE_LOGS, "Error posting to Unit4[{}], dumping dto({}): {} ", appProperties.proxy().baseUrl() + RESSURS_ENDPOINT, unit4RessursDto.getClass().getSimpleName(), body, wce);
            log.debug("Error creating unit4 ressurs", wce);

            logIfErrorMessageInBody(wce.getResponseBodyAsString(StandardCharsets.UTF_8));

            if (httpStatus == HttpStatus.CONFLICT) {
                throw new Unit4ConflictException("Ressurs finnes i Unit4. (HttpStatus.CONFLICT)");
            } else if (httpStatus.is4xxClientError()) {
                throw new Unit4RequestException("Unit4 melder klient feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else if (httpStatus.is5xxServerError()) {
                throw new Unit4ServerException("Unit4 melder server feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else {
                throw new Unit4UnknownException("Ukjent feil fra Unit4. HttpStatus: %s".formatted(httpStatus.toString()));
            }
        } catch (WebClientException wce) {
            log.warn("Unit4 createRessurs request error", wce);
            log.warn(SECURE_LOGS, "Unit4 createRessurs request error for Unit4[{}], dumping dto({}): {}", appProperties.proxy().baseUrl() + RESSURS_ENDPOINT, unit4RessursDto.getClass().getSimpleName(), body, wce);
            throw new Unit4UnknownException("Annen ukjent feil ved request til Unit4");
        }
    }

    public JsonNode patchEmployee(String personId, List<Unit4PatchRequest> unit4PatchRequest) throws Unit4Exception, JsonProcessingException{
        val body = objectMapper.writeValueAsString(unit4PatchRequest);
        val url = appProperties.proxy().baseUrl() + RESSURS_ENDPOINT + "/" + personId;
        log.debug(SECURE_LOGS, "Patch unit4 ressurs [{}]:\n{}", url, body);

        try {
            JsonNode response =
                webClient.patch()
                    .uri(url)
                    .headers(httpHeaders -> {
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        httpHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());
                        httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                    })
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(JsonNode.class)
                    .block()
                    .getBody() ;

            log.debug(SECURE_LOGS, "Response fra create unit4 ressurs:\n{}", response.toPrettyString());
            return response;
        } catch (WebClientResponseException wce) {
            HttpStatus httpStatus = wce.getStatusCode();
            log.error(SECURE_LOGS, "Error patching to Unit4[{}], dumping dto({}): {} ", url, unit4PatchRequest.getClass().getSimpleName(), body, wce);

            logIfErrorMessageInBody(wce.getResponseBodyAsString(StandardCharsets.UTF_8));

            if (httpStatus == HttpStatus.NOT_FOUND || httpStatus == HttpStatus.GONE){
                throw new Unit4NotFoundException("Ressurs finnes ikke i Unit4. HttpsStatus: $s".formatted(httpStatus.toString()));
            } else if (httpStatus.is4xxClientError()) {
                throw new Unit4RequestException("Unit4 melder klient feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else if (httpStatus.is5xxServerError()) {
                throw new Unit4ServerException("Unit4 melder server feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else {
                throw new Unit4UnknownException("Ukjent feil fra Unit4. HttpStatus: %s".formatted(httpStatus.toString()));
            }
        } catch (WebClientException wce) {
            log.warn("Unit4 patchRessurs request error", wce);
            log.warn(SECURE_LOGS, "Unit4 patchRessurs request error for Unit4[{}], dumping dto({}): {}", url, unit4PatchRequest.getClass().getSimpleName(), body, wce);
            throw new Unit4UnknownException("Annen ukjent feil ved request til Unit4");
        }
    }

    public void deleteEmployee(String personId) throws Unit4Exception {
        val url = appProperties.proxy().baseUrl() + RESSURS_ENDPOINT + "/" + personId;
        log.debug("Delete unit4 ressurs [{}]:\n{}", url);

        try {
            webClient.delete()
                .uri(url)
                .headers(httpHeaders -> {
                    httpHeaders.setContentType(UNIT4_MEDIETYPE_JSON_PATCH);
                    httpHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());
                    httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .retrieve()
                .toEntity(JsonNode.class)
                .block();

        } catch (WebClientResponseException wce) {
            HttpStatus httpStatus = wce.getStatusCode();
            log.error(SECURE_LOGS, "Error in delete Employee to Unit4[{}]", wce);

            logIfErrorMessageInBody(wce.getResponseBodyAsString(StandardCharsets.UTF_8));

            if (httpStatus == HttpStatus.NOT_FOUND || httpStatus == HttpStatus.GONE){
                throw new Unit4NotFoundException("Ressurs finnes ikke i Unit4. HttpsStatus: $s".formatted(httpStatus.toString()));
            } else if (httpStatus.is4xxClientError()) {
                throw new Unit4RequestException("Unit4 melder klient feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else if (httpStatus.is5xxServerError()) {
                throw new Unit4ServerException("Unit4 melder server feil. HttpStatus: %s".formatted(httpStatus.toString()));
            } else {
                throw new Unit4UnknownException("Ukjent feil fra Unit4. HttpStatus: %s".formatted(httpStatus.toString()));
            }
        } catch (WebClientException wce) {
            log.warn("Unit4 deleteRessurs request error", wce);
            log.warn(SECURE_LOGS, "Unit4 deleteRessurs request error for Unit4[{}]", url, wce);
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
            logIfNotificationMessagesInJson(bodyNode);
        }
    }

    private void logIfNotificationMessagesInJson(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                if (field.getKey().equals("notificationMessages")) {
                    log.warn(field.getValue().toPrettyString());
                } else {
                    logIfNotificationMessagesInJson(field.getValue());
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::logIfNotificationMessagesInJson);
        }
    }
}
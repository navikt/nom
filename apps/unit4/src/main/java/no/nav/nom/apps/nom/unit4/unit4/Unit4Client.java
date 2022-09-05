package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.nom.unit4.AppProperties;
import no.nav.nom.apps.nom.unit4.TokenRepository;
import no.nav.nom.apps.nom.unit4.input.NomRessursDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class Unit4Client {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final TokenRepository tokenRepository;

    private final AppProperties appProperties;

    private final static String RESSURS_POST_ENDPOINT = "/proxy/unit4/ressurs";

    @SneakyThrows
    public String createRessurs(NomRessursDTO nomRessursDTO){
        val requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setBasicAuth(appProperties.getUnit4().getApiUsername(), appProperties.getUnit4().getApiPassword());
        val body = objectMapper.writeValueAsString(new Unit4RessursDto(nomRessursDTO));

        val request = new HttpEntity<>(body, requestHeaders);

        return restTemplate.exchange(appProperties.getUnit4().getBaseUrl()+appProperties.getUnit4().getEmployeesPath(), HttpMethod.POST, request, String.class).getBody();
    }

    @SneakyThrows
    public ResponseEntity<String> createRessursByProxy(NomRessursDTO nomRessursDTO){
        val requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.getProxy().getAadClientId()).serialize());
        val body = objectMapper.writeValueAsString(new Unit4RessursDto(nomRessursDTO));

        val request = new HttpEntity<>(body, requestHeaders);

        return restTemplate.exchange(appProperties.getProxy().getBaseUrl() + RESSURS_POST_ENDPOINT, HttpMethod.POST, request, String.class);
    }
}

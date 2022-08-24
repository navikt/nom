package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.nom.unit4.input.NomRessursDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class Unit4Client {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String createRessurs(NomRessursDTO nomRessursDTO){
        val requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setBasicAuth("", "");
        val body = objectMapper.writeValueAsString(new Unit4RessursDto(nomRessursDTO));

        val request = new HttpEntity<>(body, requestHeaders);


        return restTemplate.exchange("https://agresso.test.local/agrt01-web-api/v1/employees", HttpMethod.POST, request, String.class).getBody();
    }
}

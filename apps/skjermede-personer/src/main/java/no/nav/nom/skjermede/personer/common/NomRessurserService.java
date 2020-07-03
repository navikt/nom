package no.nav.nom.skjermede.personer.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.dto.RessursDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NomRessurserService {

    private final RestTemplate restTemplate;

    @Value("${ressurser-api}")
    private String ressursAPI;

    public RessursDTO getInternRessursByNavident(final String navident, final boolean aktiv) {
        val headers = new HttpHeaders();
        headers.set("navident", navident);

        return restTemplate.exchange(
                String.format("%s/ansatt?gyldig=%s&ressursType=INTERN,ANNEN_STAT", ressursAPI, aktiv),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RessursDTO.class).getBody();
    }

    public RessursDTO getInternRessursByPersonident(final String personident, final boolean aktiv) {
        val headers = new HttpHeaders();
        headers.set("personident", personident);

        return restTemplate.exchange(
                String.format("%s/ansatt?gyldig=%s&ressursType=INTERN,ANNEN_STAT", ressursAPI, aktiv),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                RessursDTO.class).getBody();
    }
}

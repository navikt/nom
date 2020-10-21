package no.nav.nom.apps.nom.proxy.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/proxy", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DvhController {

    @Value("dvh.endpoint.hrog.hierarki")
    private String dvhHrogHierarkiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/HRORG_HIERARKI")
    public String requestNomEnheter(String fra, String til){
        val url = UriComponentsBuilder
                .fromUriString(dvhHrogHierarkiUrl);
        if (fra != null){
            url.queryParam("in_sekvens_fra=", fra);
        }
        if(til != null){
            url.queryParam("in_sekvens_til", til);
        }

        return restTemplate.getForObject(url.build().toUriString(), String.class);
    }

}
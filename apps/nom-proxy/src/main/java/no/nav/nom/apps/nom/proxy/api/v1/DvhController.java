package no.nav.nom.apps.nom.proxy.api.v1;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Protected
@RestController
@RequestMapping(value = "/api/v1/proxy", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DvhController {

    @Value("${dvh.endpoint.hrog.hierarki}")
    private String dvhHrogHierarkiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/NOM_HRORG_HIEARKI_PARAM")
    public String requestNomEnheter(@RequestParam(required = false) String in_sekvens_fra,
                                    @RequestParam(required = false) String in_sekvens_til){
        val url = UriComponentsBuilder
                .fromUriString(dvhHrogHierarkiUrl);
        if (in_sekvens_fra != null){
            url.queryParam("in_sekvens_fra", in_sekvens_fra);
        }
        if(in_sekvens_til != null){
            url.queryParam("in_sekvens_til", in_sekvens_til);
        }

        return restTemplate.getForObject(url.build().toUriString(), String.class);
    }

}
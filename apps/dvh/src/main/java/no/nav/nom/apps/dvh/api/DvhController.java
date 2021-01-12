package no.nav.nom.apps.dvh.api;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.dvh.common.AppProperties;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/proxy")
@RequiredArgsConstructor
public class DvhController {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    @GetMapping("/stillinger")
    public String getStillinger(){
        return restTemplate.exchange(appProperties.getDvh().getStilling().getUrl(),
                HttpMethod.GET, null, String.class).getBody();
    }
}

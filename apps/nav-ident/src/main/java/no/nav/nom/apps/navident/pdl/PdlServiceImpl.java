package no.nav.nom.apps.navident.pdl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdlServiceImpl implements PdlService {
//    private WebClient webClient;

    @Override
    public List<String> getKnownPersonider(String id) {
        // TODO: Not Implemented yet - just echo
        return Arrays.asList(id);
    }

//    public List<String> callPdlEndpoint(@RegisteredOAuth2AuthorizedClient("pdlClient") OAuth2AuthorizedClient webapiBClient, String body) {
//        return Arrays.asList();
//    }

}

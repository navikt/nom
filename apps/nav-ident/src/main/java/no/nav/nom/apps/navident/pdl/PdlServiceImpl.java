package no.nav.nom.apps.navident.pdl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.PdlConfig;
import no.nav.nom.apps.navident.common.security.AADTokenRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_CALL_ID_KEY;
import static no.nav.nom.libs.utils.common.mdc.MdcConfig.MDC_CALL_ID_KEY;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdlServiceImpl implements PdlService {

//    private final String pdlHentIdentQry;

    private final AADTokenRepository tokenRepository;
    private final WebClient pdlWebClient;
    private final PdlConfig pdlConfig;

    @Override
    public List<String> getKnownPersonider(String id) {
        // TODO: Not Implemented yet - just echo
        return Arrays.asList(id);
    }

//    private String postToPdl(String qryBody) {
//        tokenRepository.getOnbehalfofTokenFor()
//
//        return pdlWebClient
//                .post()
//                .uri(pdlConfig.getBaseUrl())
//                .header(HEADER_CALL_ID_KEY, MDC.get(MDC_CALL_ID_KEY))
//                .setBearerAuth(aadToken)
//                .body(Mono.just(qryBody), String.class)
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//    }
}

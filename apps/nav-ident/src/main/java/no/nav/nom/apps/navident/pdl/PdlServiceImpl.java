package no.nav.nom.apps.navident.pdl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.security.AADTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdlServiceImpl implements PdlService {

    private final AADTokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<String> getKnownPersonider(String id) {
        // TODO: Not Implemented yet - just echo
        return Arrays.asList(id);
    }
}

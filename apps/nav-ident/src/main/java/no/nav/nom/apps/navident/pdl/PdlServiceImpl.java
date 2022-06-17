package no.nav.nom.apps.navident.pdl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class PdlServiceImpl implements PdlService {

    @Override
    public List<String> getKnownPersonider(String id) {
        // TODO: Not Implemented yet - just echo
        return Arrays.asList(id);
    }
}

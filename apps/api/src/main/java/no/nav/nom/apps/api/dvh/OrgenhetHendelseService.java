package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetPosgresqlService;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgenhetHendelseService {

    private final OrgenhetPosgresqlService orgenhetService;

    public void handterOrgenhetFraDvh(List<Orgenhet> o) {

        val bulkSize = 10000;
        for (int i = 0; i < o.size(); i += bulkSize) {
            val selection = o.subList(i, Math.min(i + bulkSize, o.size()));
            val orgenhetByAgressoId = orgenhetService.findByAgressoIdBulk(selection.stream().map(Orgenhet::getAgressoId).toList());

            val opprettInserts = new HashMap<String, String>();
            val endreinserts = new HashMap<UUID, Map<String, String>>();

            selection.forEach(or -> {
                if (orgenhetByAgressoId.containsKey(or.getAgressoId())) {
                    var orgenhet = orgenhetByAgressoId.get(or.getAgressoId());
                    var endringer = new HashMap<String, String>();

                    if (!orgenhet.getNavn().equals(or.getNavn())) {
                        endringer.put("navn", or.getNavn());
                    }

                    if (!endringer.isEmpty()) {
                        endreinserts.put(orgenhet.getId(), endringer);
                    }
                } else {
                    opprettInserts.put(or.getAgressoId(), or.getNavn());
                }
            });

            orgenhetService.createEndreHendelser(endreinserts);
            orgenhetService.createOpprettHendelser(opprettInserts);
        }
    }
}

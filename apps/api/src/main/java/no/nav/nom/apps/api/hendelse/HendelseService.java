package no.nav.nom.apps.api.hendelse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.domain.Ressurs;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.HendelseTo.Endringstype;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HendelseService {

    private final ObjectMapper om;
    private final RessursRepository ressursRepository;
    private final HendelseProducer hendelseProducer;

    public void opprettRessurs(String navIdent, String personIdent) {
        Optional<Ressurs> existing = ressursRepository.findByPersonIdent(personIdent);
        if (existing.isPresent()) {
            return;
        }
        var navIdentRessurs = ressursRepository.findByNavIdent(navIdent);
        navIdentRessurs.ifPresent(ressurs ->
                log.warn("NavIdent {} for personIdent {} er i bruk av en annen ressurs med personIdent {}", navIdent, personIdent, ressurs.getPersonIdent())
        );

        var hendelse = createOpprettetRessursHendelse(navIdent, personIdent);
        hendelseProducer.produceHendelse(hendelse);
    }

    private HendelseTo createOpprettetRessursHendelse(String navIdent, String personIdent) {
        var data = om.createObjectNode();
        data.put("fid", UUID.randomUUID().toString());
        data.put("navIdent", navIdent);
        data.put("personIdent", personIdent);

        return HendelseTo.builder()
                .id(UUID.randomUUID())
                .reqId(UUID.randomUUID())
                .type(Hendelsestyper.RESSURS_V1.getKey())
                .data(data)
                .endringstype(Endringstype.OPPRETTET)
                .opprettet(LocalDateTime.now())
                .build();
    }

}
